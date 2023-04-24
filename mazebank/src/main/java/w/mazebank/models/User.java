package w.mazebank.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import w.mazebank.enums.RoleType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private int bsn;

    private String firstName;

    private String lastName;

    private String password;

    private String phoneNumber;

    @Enumerated(EnumType.ORDINAL)
    private RoleType role;

    private LocalDate dateOfBirth;

    private LocalDateTime createdAt;

    private boolean blocked;

    @OneToMany(mappedBy = "user")
    @JsonBackReference
    private List<Account> accounts;
}
