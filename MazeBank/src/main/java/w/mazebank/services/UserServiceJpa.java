package w.mazebank.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.RoleType;
import w.mazebank.exceptions.*;
import w.mazebank.models.Account;
import w.mazebank.models.Transaction;
import w.mazebank.models.User;
import w.mazebank.models.requests.UserPatchRequest;
import w.mazebank.models.responses.AccountResponse;
import w.mazebank.models.responses.BalanceResponse;
import w.mazebank.models.responses.TransactionResponse;
import w.mazebank.models.responses.UserResponse;
import w.mazebank.repositories.TransactionRepository;
import w.mazebank.repositories.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class UserServiceJpa extends BaseServiceJpa {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public User getUserById(Long id) throws UserNotFoundException {
        if (id == 1) throw new UnauthorizedUserAccessException("You are not allowed to access the bank");
        else
            return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("user not found with id: " + id));
    }

    public User getUserByIdAndValidate(Long id, User userPerforming) throws UserNotFoundException {
        if (id == 1) throw new UnauthorizedUserAccessException("You are not allowed to access the bank");

        // check if user id is the same as the user performing the request or if the user performing the request is an employee and not blocked
        if (userPerforming.getId() != id && (!userPerforming.getRole().equals(RoleType.EMPLOYEE) || userPerforming.isBlocked())) {
            throw new UserNotFoundException("user not found with id: " + id);
        }
        return getUserById(id);
    }


    public List<AccountResponse> getAccountsByUserId(Long userId, User userPerforming) throws UserNotFoundException, UnauthorizedAccountAccessException {
        if (userId == 1) throw new UnauthorizedUserAccessException("You are not allowed to access the bank");

        // throw exception if user is not an employee and not the user performing the request
        if (!userPerforming.getRole().equals(RoleType.EMPLOYEE) && userPerforming.getId() != userId) {
            throw new UnauthorizedAccountAccessException("user not allowed to access accounts of user with id: " + userId);
        }

        // get user
        User user = getUserById(userId);

        // get accounts from user
        List<Account> accounts = user.getAccounts();
        if (accounts == null) return new ArrayList<>();

        // parse accounts to account responses
        List<AccountResponse> accountResponses = new ArrayList<>();
        for (Account account : accounts) {
            AccountResponse accountResponse = AccountResponse.builder()
                .id(account.getId())
                .accountType(account.getAccountType().getValue())
                .iban(account.getIban())
                .balance(account.getBalance())
                .timestamp(account.getCreatedAt().toString())
                .build();
            accountResponses.add(accountResponse);
        }
        // return account responses
        return accountResponses;
    }

    public List<UserResponse> getAllUsers(int pageNumber, int pageSize, String sort, String search, boolean withoutAccounts) {
        List<User> users = findAllPaginationAndSort(pageNumber, pageSize, sort, search, userRepository);

        List<User> filteredUsers = new ArrayList<>(users);

        // If withoutAccounts is true, remove users that have accounts
        if (withoutAccounts) {
            filteredUsers.removeIf(user -> user.getAccounts() != null && !user.getAccounts().isEmpty());
        }

        // Parse users to user responses
        List<UserResponse> userResponses = new ArrayList<>();
        for (User user : filteredUsers) {
            UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
            userResponses.add(userResponse);
        }

        return userResponses;
    }


    public void addUser(User user) {
        userRepository.save(user);
    }

    public void blockUser(Long id) throws UserNotFoundException {
        if (id == 1) throw new UnauthorizedUserAccessException("You are not allowed to access the bank");

        User user = getUserById(id);
        user.setBlocked(true);

        userRepository.save(user);
    }

    public void unblockUser(Long id) throws UserNotFoundException {
        if (id == 1) throw new UnauthorizedUserAccessException("You are not allowed to access the bank");

        User user = getUserById(id);
        user.setBlocked(false);

        userRepository.save(user);
    }

    public User patchUserById(long id, UserPatchRequest userPatchRequest, User userPerforming) throws UserNotFoundException, DisallowedFieldException {
        if (id == 1) throw new UnauthorizedUserAccessException("You are not allowed to access the bank");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) throw new UserNotFoundException("user not found with id: " + id);

        // check if user is the same as the user performing the request or if the user performing the request is an employee
        if (userPerforming.getId() != id && !userPerforming.getRole().equals(RoleType.EMPLOYEE)) {
            throw new UnauthorizedUserAccessException("user not allowed to access user with id: " + id);
        }

        List<String> allowedFields = Arrays.asList("email", "firstName", "lastName", "phoneNumber");

        // check if fields are allowed
        for (String field : userPatchRequest.getFields()) {
            if (!allowedFields.contains(field))
                throw new DisallowedFieldException("field not allowed to update: " + field);
        }

        // PATCHES AVAILABLE FOR CUSTOMERS
        if (userPatchRequest.getEmail() != null) user.setEmail(userPatchRequest.getEmail());
        if (userPatchRequest.getFirstName() != null) user.setFirstName(userPatchRequest.getFirstName());
        if (userPatchRequest.getLastName() != null) user.setLastName(userPatchRequest.getLastName());
        if (userPatchRequest.getPhoneNumber() != null) user.setPhoneNumber(userPatchRequest.getPhoneNumber());

        // PATCHES AVAILABLE FOR EMPLOYEES
        if ((userPatchRequest.getTransactionLimit() != null || userPatchRequest.getDayLimit() != null) && userPerforming.getRole() != RoleType.EMPLOYEE) {
            throw new UnauthorizedUserAccessException("You are not allowed to update the transaction limit or day limit");
        }

        if (userPatchRequest.getTransactionLimit() != null && userPerforming.getRole() == RoleType.EMPLOYEE)
            user.setTransactionLimit(userPatchRequest.getTransactionLimit());
        if (userPatchRequest.getDayLimit() != null && userPerforming.getRole() == RoleType.EMPLOYEE)
            user.setDayLimit(userPatchRequest.getDayLimit());

        userRepository.save(user);

        return user;
    }

    public void deleteUserById(Long id)
        throws UserNotFoundException, UserHasAccountsException {
        if (id == 1) throw new UnauthorizedUserAccessException("You are not allowed to delete the bank");
        User user = getUserById(id);

        // if user has accounts, cannot delete user
        if (user.getAccounts() != null && !user.getAccounts().isEmpty())
            throw new UserHasAccountsException("user has accounts, cannot delete user");

        userRepository.delete(user);
    }


    public List<TransactionResponse> getTransactionsByUserId(Long userId, User user, int pageNumber, int pageSize, String sort, Optional<String> fromIban, Optional<String> toIban, Optional<LocalDate> startDate, Optional<LocalDate> endDate, Optional<Double> maxAmount, Optional<Double> minAmount, Optional<Double> amount
    ) throws UserNotFoundException {
        User requestedUser = getUserByIdAndValidate(userId, user);
        // initial empty Specification to build the dynamic query conditions.
        Specification<Transaction> specification = Specification.where(null);

        // If present, it adds conditions to filter transactions
        fromIban.ifPresent(value -> addFromIbanCondition(specification, value));
        toIban.ifPresent(value -> addToIbanCondition(specification, value));
        startDate.ifPresent(value -> addStartDateCondition(specification, value));
        endDate.ifPresent(value -> addEndDateCondition(specification, value));
        maxAmount.ifPresent(value -> addMaxAmountCondition(specification, value));
        minAmount.ifPresent(value -> addMinAmountCondition(specification, value));
        amount.ifPresent(value -> addAmountCondition(specification, value));

        // adds a condition to filter transactions by the requested user.
        addRequestedUserCondition(specification, requestedUser);

        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "timestamp"));

        Page<Transaction> transactionPage = transactionRepository.findAll(specification, pageable);
        List<Transaction> transactions = transactionPage != null ? transactionPage.getContent() : Collections.emptyList();

        return mapTransactionsToResponses(transactions);
    }

    private void addFromIbanCondition(Specification<Transaction> specification, String fromIban) {
        specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("sender").get("iban")),
                "%" + fromIban.toLowerCase() + "%"
            )
        );
    }

    private void addToIbanCondition(Specification<Transaction> specification, String toIban) {
        specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("receiver").get("iban")),
                "%" + toIban.toLowerCase() + "%"
            )
        );
    }

    private void addStartDateCondition(Specification<Transaction> specification, LocalDate startDate) {
        specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), startDate.atStartOfDay())
        );
    }

    private void addEndDateCondition(Specification<Transaction> specification, LocalDate endDate) {
        specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), endDate.atTime(LocalTime.MAX))
        );
    }

    private void addMaxAmountCondition(Specification<Transaction> specification, Double maxAmount) {
        specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount)
        );
    }

    private void addMinAmountCondition(Specification<Transaction> specification, Double minAmount) {
        specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount)
        );
    }

    private void addAmountCondition(Specification<Transaction> specification, Double amount) {
        specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("amount"), amount)
        );
    }

    private void addRequestedUserCondition(Specification<Transaction> specification, User requestedUser) {
       specification.and((root, query, criteriaBuilder) ->
            criteriaBuilder.or(
                criteriaBuilder.equal(root.get("sender"), requestedUser),
                criteriaBuilder.equal(root.get("receiver"), requestedUser)
            )
        );
    }

    private List<TransactionResponse> mapTransactionsToResponses(List<Transaction> transactions) {
        List<TransactionResponse> transactionResponses = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionResponse transactionResponse = TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .sender(transaction.getSender().getIban())
                .receiver(transaction.getReceiver().getIban())
                .timestamp(transaction.getTimestamp().toString())
                .build();
            transactionResponses.add(transactionResponse);
        }
        return transactionResponses;
    }

    public BalanceResponse getBalanceByUserId(Long userId, User userPerforming) throws UserNotFoundException {
        if (userId == 1) throw new UnauthorizedUserAccessException("You are not allowed to access the bank");

        // check if userId is from a user that is a existing user
        // and validate if the performing user has the rights to access the user
        User user = getUserByIdAndValidate(userId, userPerforming);
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setUserId(userId);

        // calculate total balance and set checking and savings balance if account exists
        for (Account account : user.getAccounts()) {
            if (account.getAccountType() == AccountType.CHECKING) {
                balanceResponse.setCheckingBalance(account.getBalance());
            } else if (account.getAccountType() == AccountType.SAVINGS) {
                balanceResponse.setSavingsBalance(account.getBalance());
            }
            // add balance to total balance
            balanceResponse.setTotalBalance(balanceResponse.getTotalBalance() + account.getBalance());
        }

        return balanceResponse;
    }
}
