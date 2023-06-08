package w.mazebank.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.RoleType;
import w.mazebank.enums.TransactionType;
import w.mazebank.exceptions.*;
import w.mazebank.models.Account;
import w.mazebank.models.Transaction;
import w.mazebank.models.User;
import w.mazebank.models.requests.TransactionRequest;
import w.mazebank.models.responses.TransactionResponse;
import w.mazebank.repositories.AccountRepository;
import w.mazebank.repositories.TransactionRepository;

import java.time.LocalDateTime;

@Service
public class TransactionServiceJpa {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    @Lazy
    private AccountServiceJpa accountServiceJpa;

    private final ModelMapper mapper = new ModelMapper();

    public TransactionResponse getTransactionAndValidate(Long id, User user) throws TransactionNotFoundException {
        Transaction transaction = getTransactionById(id);
        checkIfSenderIsBankAccount(transaction);
        checkIfUserIsTransactionParticipant(user, transaction);
        return mapTransactionToResponse(transaction);
    }

    private void checkIfSenderIsBankAccount(Transaction transaction) throws UnauthorizedTransactionAccessException {
        String bankIban = "NL01INHO0000000001";
        if (transaction.getSender().getIban().equals(bankIban)) {
            throw new UnauthorizedTransactionAccessException("You are not allowed to access transactions of the bank's bank account");
        }
    }

    private TransactionResponse mapTransactionToResponse(Transaction transaction) {
        mapper.typeMap(Transaction.class, TransactionResponse.class)
            .addMappings(mapper -> {
                mapper.map(src -> src.getSender().getIban(), TransactionResponse::setSender);
                mapper.map(src -> src.getReceiver().getIban(), TransactionResponse::setReceiver);
            });
        return mapper.map(transaction, TransactionResponse.class);
    }

    @Transactional
    public TransactionResponse postTransaction(TransactionRequest transactionRequest, User userPerforming)
        throws TransactionFailedException, InsufficientFundsException, AccountNotFoundException {

        if (transactionRequest.getSenderIban().equals("NL01INHO0000000001")) {
            throw new UnauthorizedAccountAccessException("You are not allowed to perform transactions for the bank's bank account");
        }

        Account senderAccount = accountServiceJpa.getAccountByIban(transactionRequest.getSenderIban());
        Account receiverAccount = accountServiceJpa.getAccountByIban(transactionRequest.getReceiverIban());

        Transaction transaction = Transaction.builder()
            .amount(transactionRequest.getAmount())
            .description(transactionRequest.getDescription())
            .transactionType(TransactionType.TRANSFER)
            .userPerforming(userPerforming)
            .sender(senderAccount)
            .receiver(receiverAccount)
            .timestamp(LocalDateTime.now())
            .build();

        validateRegularTransaction(transaction);

        updateAccountBalances(senderAccount, receiverAccount, transactionRequest.getAmount());

        return performTransaction(transaction);
    }

    private Transaction getTransactionById(Long id) throws TransactionNotFoundException {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new TransactionNotFoundException("Transaction with id: " + id + " not found"));
    }

    private void checkIfUserIsTransactionParticipant(User user, Transaction transaction) {
        if (user.getRole() == RoleType.EMPLOYEE) {
            return;
        }

        if (transaction.getSender().getUser().getId() == user.getId()
            || transaction.getReceiver().getUser().getId() == user.getId()) {
            return;
        }

        throw new UnauthorizedTransactionAccessException("User with id: " + user.getId() + " is not authorized to access transaction with id: " + transaction.getId());
    }

    private Account getBankAccount() throws AccountNotFoundException {
        return accountServiceJpa.getAccountByIban("NL01INHO0000000001");
    }

    public void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    private TransactionResponse performTransaction(Transaction transaction) {
        transactionRepository.save(transaction);

        return new TransactionResponse(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getDescription(),
            transaction.getSender().getIban(),
            transaction.getReceiver().getIban(),
            transaction.getUserPerforming().getId(),
            transaction.getTimestamp().toString(),
            transaction.getTransactionType().toString());
    }

    @Transactional
    public TransactionResponse atmAction(Account account, double amount, TransactionType transactionType, User userPerforming) throws TransactionFailedException, AccountNotFoundException {
        Transaction transaction = Transaction.builder()
            .amount(amount)
            .transactionType(transactionType)
            .userPerforming(userPerforming)
            .sender(getBankAccount())
            .receiver(account)
            .timestamp(java.time.LocalDateTime.now())
            .build();

        validateAtmTransaction(transaction);

        updateAccountBalanceForAtmAction(account, amount, transactionType);

        return performTransaction(transaction);
    }

    private void validateTransaction(Transaction transaction) throws TransactionFailedException {
        checkIfSenderAndReceiverAreNotTheSame(transaction);
        checkIfOneOfTheAccountsIsBlocked(transaction);
        checkIfTransactionLimitIsExceeded(transaction);
        checkIfAbsoluteLimitIsReached(transaction);
        checkDayLimitExceeded(transaction);
        checkIfUserIsBlocked(transaction);
    }

    private void validateAtmTransaction(Transaction transaction) throws TransactionFailedException {
        validateTransaction(transaction);

        if (transaction.getReceiver().getAccountType() == AccountType.SAVINGS) {
            throw new TransactionFailedException("Cannot deposit or withdraw to a savings account from an ATM");
        }
    }

    private void validateRegularTransaction(Transaction transaction) throws TransactionFailedException, InsufficientFundsException {
        validateTransaction(transaction);

        savingsAccountCheckSend(transaction);

        checkIfUserIsAuthorized(transaction);
    }

    private void checkIfOneOfTheAccountsIsBlocked(Transaction transaction) throws TransactionFailedException {
        if (!transaction.getSender().isActive()) {
            throw new TransactionFailedException("Sender account is blocked");
        }

        if (!transaction.getReceiver().isActive()) {
            throw new TransactionFailedException("Receiver account is blocked");
        }
    }

    private void checkIfUserIsBlocked(Transaction transaction) throws TransactionFailedException {
        User user = transaction.getSender().getUser();

        if (user.isBlocked()) {
            throw new TransactionFailedException("User is blocked");
        }
    }

    private void checkIfUserIsAuthorized(Transaction transaction) throws TransactionFailedException {
        if (transaction.getUserPerforming().getRole() != RoleType.EMPLOYEE
            && transaction.getUserPerforming().getId() != transaction.getSender().getUser().getId()) {
            throw new TransactionFailedException("User performing the transaction is not authorized to perform this transaction");
        }
    }

    private void checkIfTransactionLimitIsExceeded(Transaction transaction) throws TransactionFailedException {
        if (transaction.getAmount() > transaction.getSender().getUser().getTransactionLimit()) {
            throw new TransactionFailedException("Transaction limit exceeded");
        }
    }

    private void checkIfAbsoluteLimitIsReached(Transaction transaction) throws AccountAbsoluteLimitReachedException {
        if (transaction.getTransactionType() == TransactionType.TRANSFER
            || transaction.getTransactionType() == TransactionType.WITHDRAWAL) {
            Account accountToCheck = transaction.getTransactionType() == TransactionType.WITHDRAWAL
                ? transaction.getReceiver()
                : transaction.getSender();

            if ((accountToCheck.getBalance() - transaction.getAmount()) < accountToCheck.getAbsoluteLimit()) {
                throw new AccountAbsoluteLimitReachedException("Balance cannot become lower than absolute limit");
            }
        }
    }

    private void checkDayLimitExceeded(Transaction transaction) throws TransactionFailedException {
        if (dayLimitExceeded(transaction.getSender(), transaction.getAmount())) {
            throw new TransactionFailedException("Day limit exceeded");
        }
    }

    private void savingsAccountCheckSend(Transaction transaction) throws TransactionFailedException {
        if (transaction.getSender().getAccountType() == AccountType.SAVINGS
            && (transaction.getSender().getUser().getId() != transaction.getReceiver().getUser().getId())) {
            throw new TransactionFailedException("Cannot transfer from a savings account to an account that is not of the same customer");
        }

        if (transaction.getReceiver().getAccountType() == AccountType.SAVINGS
            && (transaction.getSender().getUser().getId() != transaction.getReceiver().getUser().getId())) {
            throw new TransactionFailedException("Cannot transfer to a savings account from an account that is not of the same customer");
        }
    }

    private void checkIfSenderAndReceiverAreNotTheSame(Transaction transaction) throws TransactionFailedException {
        if (transaction.getSender().getId() == transaction.getReceiver().getId()) {
            throw new TransactionFailedException("Sender and receiver cannot be the same");
        }
    }

    private boolean dayLimitExceeded(Account sender, double amount) {
        Double totalAmountOfTransactionForToday = transactionRepository.getTotalAmountOfTransactionForToday(sender.getId());
        double currentTotal = totalAmountOfTransactionForToday != null ? totalAmountOfTransactionForToday : 0.0;

        double dayLimit = sender.getUser().getDayLimit();
        return currentTotal + amount > dayLimit;
    }

    private void updateAccountBalances(Account senderAccount, Account receiverAccount, double amount) {
        senderAccount.setBalance(senderAccount.getBalance() - amount);
        receiverAccount.setBalance(receiverAccount.getBalance() + amount);

        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);
    }

    private void updateAccountBalanceForAtmAction(Account account, double amount, TransactionType transactionType) {
        if (transactionType == TransactionType.WITHDRAWAL) {
            account.setBalance(account.getBalance() - amount);
        } else if (transactionType == TransactionType.DEPOSIT) {
            account.setBalance(account.getBalance() + amount);
        }

        accountRepository.save(account);
    }
}
