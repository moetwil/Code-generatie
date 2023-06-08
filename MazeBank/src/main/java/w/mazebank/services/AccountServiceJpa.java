package w.mazebank.services;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.RoleType;
import w.mazebank.enums.TransactionType;
import w.mazebank.exceptions.*;
import w.mazebank.models.Account;
import w.mazebank.models.Transaction;
import w.mazebank.models.User;
import w.mazebank.models.requests.AccountPatchRequest;
import w.mazebank.models.requests.AccountRequest;
import w.mazebank.models.responses.AccountResponse;
import w.mazebank.models.responses.IbanResponse;
import w.mazebank.models.responses.TransactionResponse;
import w.mazebank.models.responses.UserResponse;
import w.mazebank.repositories.AccountRepository;
import w.mazebank.repositories.TransactionRepository;
import w.mazebank.utils.IbanGenerator;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountServiceJpa extends BaseServiceJpa {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionServiceJpa transactionServiceJpa;

    @Autowired
    private UserServiceJpa userServiceJpa;

    @Autowired
    private TransactionRepository transactionRepository;

    private final ModelMapper mapper = new ModelMapper();

    public void addAccount(Account account) {
        accountRepository.save(account);
    }

    public List<AccountResponse> getAllAccounts(int pageNumber, int pageSize, String sort, String search) {
        //
        List<Account> accounts = findAllPaginationAndSort(pageNumber, pageSize, sort, search, accountRepository);

        // map all accounts to account responses
        List<AccountResponse> accountResponses = new ArrayList<>(accounts.size());
        for (Account account : accounts) {
            AccountResponse accountResponse = createAccountResponse(account);
            accountResponses.add(accountResponse);
        }

        return accountResponses;
    }

    private Account buildAccount(AccountType accountType, User user, boolean isActive, Double absoluteLimit) {
        return Account.builder()
            .accountType(accountType)
            .iban(IbanGenerator.generate())
            .isActive(isActive)
            .user(user)
            .absoluteLimit(absoluteLimit)
            .balance(0.0)
            .build();
    }

    private AccountResponse createAccountResponse(Account account) {
        return AccountResponse.builder()
            .id(account.getId())
            .accountType(account.getAccountType().getValue())
            .iban(account.getIban())
            .user(createUserResponse(account.getUser()))
            .balance(account.getBalance())
            .absoluteLimit(account.getAbsoluteLimit())
            .active(account.isActive())
            .timestamp(account.getCreatedAt().toString())
            .build();
    }

    private UserResponse createUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .build();
    }

    public Account getAccountById(Long id) throws AccountNotFoundException {
        return accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException("Account with id: " + id + " not found"));
    }

    public Account getAccountByIban(String iban) throws AccountNotFoundException {
        return accountRepository.findByIban(iban)
            .orElseThrow(() -> new AccountNotFoundException("Account with iban: " + iban + " not found"));
    }

    public AccountResponse updateAccount(long id, AccountPatchRequest body) throws AccountNotFoundException {
        Account account = getAccountById(id);

        // Check if the account is a bank account
        if (account.getIban().equals("NL01INHO0000000001")) {
            throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");
        }

        if (body.getAbsoluteLimit() != null) {
            account.setAbsoluteLimit(body.getAbsoluteLimit());
        }

        Account updatedAccount = accountRepository.save(account);

        // Map account to account response
        AccountResponse accountResponse = mapper.map(updatedAccount, AccountResponse.class);
        return accountResponse;
    }

    private List<IbanResponse> getAccountsByOneName(String name) {
        List<Account> accounts = accountRepository.findAccountsByOneName(name);

        List<IbanResponse> ibanResponses = new ArrayList<>();
        for (Account account : accounts) {
            // skip bank account
            if (account.getIban().equals("NL01INHO0000000001")) {
                continue;
            }

            ibanResponses.add(createIbanResponse(account));
        }
        return ibanResponses;
    }

    private IbanResponse createIbanResponse(Account account){
        return IbanResponse.builder()
            .iban(account.getIban())
            .firstName(account.getUser().getFirstName())
            .lastName(account.getUser().getLastName())
            .build();
    }

    // public AccountResponse createAccount(AccountRequest body) throws UserNotFoundException, AccountCreationLimitReachedException {
    //     User user = userServiceJpa.getUserById(body.getUserId());
    //
    //     AccountType accountType = body.getAccountType();
    //     List<Account> accounts = user.getAccounts();
    //
    //     int checkingAccounts = (int) accounts.stream().filter(a -> a.getAccountType() == AccountType.CHECKING).count();
    //     int savingsAccounts = (int) accounts.stream().filter(a -> a.getAccountType() == AccountType.SAVINGS).count();
    //
    //     // Check account creation limits
    //     if (accountType == AccountType.SAVINGS) {
    //         if (checkingAccounts == 0) {
    //             throw new AccountCreationLimitReachedException("You need a checking account to create a savings account");
    //         }
    //         if (savingsAccounts >= 1) {
    //             throw new AccountCreationLimitReachedException("Savings account creation limit reached");
    //         }
    //     } else if (accountType == AccountType.CHECKING) {
    //         if (checkingAccounts >= 1) {
    //             throw new AccountCreationLimitReachedException("Checking account creation limit reached");
    //         }
    //     }
    //
    //     Account account = Account.builder()
    //         .accountType(accountType)
    //         .iban(IbanGenerator.generate())
    //         .isActive(body.isActive())
    //         .user(user)
    //         .absoluteLimit(body.getAbsoluteLimit())
    //         .balance(0.0)
    //         .build();
    //
    //     Account newAccount = accountRepository.save(account);
    //
    //     // Map account to account response
    //     AccountResponse accountResponse = mapper.map(newAccount, AccountResponse.class);
    //     accountResponse.setAccountType(accountType);
    //
    //     return accountResponse;
    // }

    public AccountResponse createAccount(AccountRequest body) throws UserNotFoundException, AccountCreationLimitReachedException {
        // Get user and account type from request body
        User user = userServiceJpa.getUserById(body.getUserId());
        AccountType accountType = body.getAccountType();
        List<Account> accounts = user.getAccounts();

        // Check account creation limits
        validateAccountCreationLimits(accountType, accounts);

        // Create account and save it to the database
        Account account = buildAccount(accountType, user, body.isActive(), body.getAbsoluteLimit());
        Account newAccount = accountRepository.save(account);

        return createAccountResponse(newAccount);
    }

    private void validateAccountCreationLimits(AccountType accountType, List<Account> accounts) throws AccountCreationLimitReachedException {
        // Count the number of checking and savings accounts
        long checkingAccounts = accounts.stream()
            .filter(a -> a.getAccountType() == AccountType.CHECKING)
            .count();

        // Count the number of savings accounts
        long savingsAccounts = accounts.stream()
            .filter(a -> a.getAccountType() == AccountType.SAVINGS)
            .count();

        // Check account creation limits
        if (accountType == AccountType.SAVINGS) {
            if (checkingAccounts == 0) {
                throw new AccountCreationLimitReachedException("You need a checking account to create a savings account");
            }
            if (savingsAccounts >= 1) {
                throw new AccountCreationLimitReachedException("Savings account creation limit reached");
            }
        } else if (accountType == AccountType.CHECKING && checkingAccounts >= 1) {
            throw new AccountCreationLimitReachedException("Checking account creation limit reached");
        }
    }







    // TODO





    public Account getAccountAndValidate(Long accountId, User user) throws AccountNotFoundException {
        Account account = getAccountById(accountId);
        validateAccountOwner(user, account);
        return account;
    }



    public TransactionResponse deposit(Long accountId, double amount, User userDetails) throws AccountNotFoundException, InvalidAccountTypeException, TransactionFailedException {
        // get account from database and validate owner
        Account account = getAccountAndValidate(accountId, userDetails);

        // use transaction service to deposit money
        return transactionServiceJpa.atmAction(account, amount, TransactionType.DEPOSIT, userDetails);
    }


    public TransactionResponse withdraw(Long accountId, double amount, User userDetails) throws AccountNotFoundException, InvalidAccountTypeException, TransactionFailedException {
        // get account from database and validate owner
        Account account = getAccountAndValidate(accountId, userDetails);


        // CHECKS:
        // check if it is a checking account

        // use transaction service to withdraw money
        return transactionServiceJpa.atmAction(account, amount, TransactionType.WITHDRAWAL, userDetails);
    }

    // private static void verifySufficientFunds(double amount, Account account) {
    //     // check if account has enough money
    //     if (account.getBalance() < amount) {
    //         throw new InsufficientFundsException("Not enough funds in account");
    //     }
    // }

    private void validateAccountOwner(User user, Account account) {

        System.out.println(user.getId());
        System.out.println(account.getUser().getId());

        // check if current user is the same as account owner or if current user is an employee
        if (user.getRole() != RoleType.EMPLOYEE && user.getId() != account.getUser().getId()) {
            throw new UnauthorizedAccountAccessException("You are not authorized to access this account");
        }
    }

    public Account lockAccount(Long id) throws AccountNotFoundException, AccountLockOrUnlockStatusException {

        if (id == 1) throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");
        if (!getAccountById(id).isActive()) {
            throw new AccountLockOrUnlockStatusException("Account is already locked");
        }
        Account account = getAccountById(id);
        account.setActive(false);

        accountRepository.save(account);
        return account;
    }

    public Account unlockAccount(Long id) throws AccountNotFoundException, AccountLockOrUnlockStatusException {
        if (id == 1) throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");

        if (getAccountById(id).isActive()) {
            throw new AccountLockOrUnlockStatusException("Account is already unlocked");
        }

        Account account = getAccountById(id);
        account.setActive(true);

        accountRepository.save(account);
        return account;
    }

    public List<IbanResponse> getAccountsByName(String name) {
        String[] names = name.split(" ");
        return names.length == 2
            ? getAccountsByFirstNameAndLastName(names[0], names[1])
            : getAccountsByOneName(name);
    }



    private List<IbanResponse> getAccountsByFirstNameAndLastName(String firstName, String lastName) {
        List<Account> accounts = accountRepository.findAccountsByFirstNameAndLastName(firstName, lastName);
        List<IbanResponse> ibanResponses = new ArrayList<>();
        for (Account account : accounts) {
            if (account.getIban().equals("NL01INHO0000000001")) continue;
            ibanResponses.add(IbanResponse.builder()
                .iban(account.getIban())
                .firstName(account.getUser().getFirstName())
                .lastName(account.getUser().getLastName())
                .build());
        }
        return ibanResponses;
    }

    public List<TransactionResponse> getTransactionsFromAccount(int pageNumber, int pageSize, String sort, User user, Long accountId) throws AccountNotFoundException {
        if (accountId == 1) throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");

        getAccountAndValidate(accountId, user);

        Sort sortObject = Sort.by(Sort.Direction.fromString(sort), "timestamp");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortObject);

        List<Transaction> transactions = transactionRepository.findBySenderIdOrReceiverId(accountId, accountId, pageable);

        // parse transactions to transaction responses
        List<TransactionResponse> transactionResponses = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionResponse response = TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .sender(transaction.getSender() != null ? transaction.getSender().getIban() : null)
                .receiver(transaction.getReceiver() != null ? transaction.getReceiver().getIban() : null)
                .transactionType(transaction.getTransactionType().name())
                .timestamp(transaction.getTimestamp().toString())
                .build();
            transactionResponses.add(response);
        }
        return transactionResponses;
    }
}