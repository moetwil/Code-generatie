package w.mazebank.models;

import lombok.Data;
import w.mazebank.enums.TransactionType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String description;

    private double amount;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Account sender;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Account receiver;

    @Enumerated(EnumType.ORDINAL)
    private TransactionType transactionType;

    private LocalDateTime createdAt;
}
