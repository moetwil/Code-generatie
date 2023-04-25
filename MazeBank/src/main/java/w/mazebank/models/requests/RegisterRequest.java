package w.mazebank.models.requests;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import w.mazebank.enums.RoleType;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String email;
    private int bsn;
    private String firstName;
    private String lastName;
    private String password;
    private String phoneNumber;
    private LocalDate dateOfBirth;
}
