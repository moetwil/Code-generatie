package w.mazebank.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import w.mazebank.enums.TransactionType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    private Long id;
    private double amount;
    private String description;
    private String sender;
    private String receiver;
    private Long userPerforming;
    private LocalDateTime timestamp;
    private String type;
}
