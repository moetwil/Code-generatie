package w.mazebank.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import w.mazebank.enums.AccountType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {
    private long id;
    private int accountType;
    private String iban;
    private UserResponse user;
    private double balance;
    private LocalDateTime createdAt;

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType.getValue();
    }
}
