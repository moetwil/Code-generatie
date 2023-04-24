package w.mazebank.seeder;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.RoleType;
import w.mazebank.enums.TransactionType;
import w.mazebank.models.Account;
import w.mazebank.models.Transaction;
import w.mazebank.models.User;
import w.mazebank.services.AccountService;
import w.mazebank.services.TransactionService;
import w.mazebank.services.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements ApplicationRunner {
    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // Create some sample users
        User user1 = new User(1, "user1@example.com", 123456789, "John", "Doe", "password1", "1234567890", RoleType.CUSTOMER, LocalDate.now().minusYears(25), LocalDateTime.now(), false, null);
        User user2 = new User(2, "user2@example.com", 987654321, "Jane", "Smith", "password2", "0987654321", RoleType.CUSTOMER, LocalDate.now().minusYears(30), LocalDateTime.now(), false, null);

        userService.addUser(user1);
        userService.addUser(user2);

        // Create some sample accounts for user1
        Account account1 = new Account(1, "NL01MAZE0000000001", AccountType.CHECKING, 1000.0, user1, true, LocalDateTime.now(), 1000.0, 5000.0, 10000.0, null, null);
        Account account2 = new Account(2, "NL01MAZE0000000002", AccountType.SAVINGS, 5000.0, user1, true, LocalDateTime.now(), 2000.0, 10000.0, 20000.0, null, null);

        accountService.addAccount(account1);
        accountService.addAccount(account2);

        // Create some sample accounts for user2
        Account account3 = new Account(3, "NL01MAZE0000000003", AccountType.CHECKING, 2000.0, user2, true, LocalDateTime.now(), 1500.0, 7500.0, 15000.0, null, null);
        Account account4 = new Account(4, "NL01MAZE0000000004", AccountType.SAVINGS, 10000.0, user2, true, LocalDateTime.now(), 5000.0, 25000.0, 50000.0, null, null);

        accountService.addAccount(account3);
        accountService.addAccount(account4);

        // Perform some transactions between the accounts
        transactionService.transferMoney(new Transaction(1, "Transfer from account1 to account3", 500.0, account1, account3, TransactionType.TRANSFER, LocalDateTime.now()));
        transactionService.transferMoney(new Transaction(2, "Transfer from account2 to account4", 2000.0, account2, account4, TransactionType.TRANSFER, LocalDateTime.now()));
    }
}
