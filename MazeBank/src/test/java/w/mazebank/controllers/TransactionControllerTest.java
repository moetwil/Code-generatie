package w.mazebank.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.TransactionType;
import w.mazebank.exceptions.TransactionFailedException;
import w.mazebank.exceptions.TransactionNotFoundException;
import w.mazebank.exceptions.UnauthorizedTransactionAccessException;
import w.mazebank.models.Account;
import w.mazebank.models.User;
import w.mazebank.models.requests.TransactionRequest;
import w.mazebank.models.responses.TransactionResponse;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class TransactionControllerTest extends BaseControllerTest {
    private TransactionRequest transactionRequest;
    private TransactionResponse transactionResponse;

    TransactionControllerTest() {
        super();
        transactionRequest = TransactionRequest.builder()
            .description("Test transaction")
            .amount(100.00)
            .senderIban("NL01INHO0000000002")
            .receiverIban("NL01INHO0000000003")
            .build();

        transactionResponse = TransactionResponse.builder()
            .id(1L)
            .description("Test transaction")
            .amount(100.00)
            .userPerforming(1L)
            .sender("NL01INHO0000000002")
            .receiver("NL01INHO0000000003")
            .transactionType(TransactionType.TRANSFER.toString())
            .timestamp(LocalDateTime.now().toString())
            .build();
    }

    @Test
    void transactionPostStatus201CreatedAndReturnsObject() throws Exception {

        Account sender = Account.builder()
            .id(1)
            .accountType(AccountType.CHECKING)
            .balance(100.00)
            .isActive(true)
            .iban("NL01INHO0000000002")
            .absoluteLimit(0.0)
            .build();

        Account receiver = Account.builder()
            .id(2)
            .accountType(AccountType.SAVINGS)
            .balance(0.00)
            .isActive(true)
            .iban("NL01INHO0000000003")
            .absoluteLimit(0.0)
            .build();


        when(transactionServiceJpa.postTransaction(Mockito.any(TransactionRequest.class), Mockito.any(User.class))).thenReturn(transactionResponse);

        // mock mvc
        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + employeeToken)
                .with(csrf())
                .with(user(authEmployee))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(transactionRequest))

            )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Test transaction"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(100.00))
            .andExpect(MockMvcResultMatchers.jsonPath("$.userPerforming").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.sender").value("NL01INHO0000000002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.receiver").value("NL01INHO0000000003"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.transactionType").value("TRANSFER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andReturn();
    }

    @Test
    void transactionFailedStatus400() throws Exception {
        TransactionRequest transactionRequest = TransactionRequest.builder()
            .description("Test transaction")
            .amount(100.00)
            .senderIban("NL01INHO0000000002")
            .receiverIban("NL01INHO0000000002")
            .build();

        when(transactionServiceJpa.postTransaction(Mockito.any(TransactionRequest.class), Mockito.any(User.class))).thenThrow(new TransactionFailedException("Sender and receiver cannot be the same"));

        // mock mvc
        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + customerToken)
                .with(csrf())
                .with(user(authCustomer))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(transactionRequest))

            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Sender and receiver cannot be the same"))
            .andReturn();
    }

    @Test
    void getTransactionByIdStatus200OKAndReturnObjects() throws Exception {

        when(transactionServiceJpa.getTransactionAndValidate(Mockito.anyLong(), Mockito.any(User.class))).thenReturn(transactionResponse);

        // mock mvc
        // Perform the GET request to fetch the transaction by ID
        mockMvc.perform(get("/transactions/{id}", 1L)
                .header("Authorization", "Bearer " + employeeToken)
                .with(csrf())
                .with(user(authEmployee))
                .contentType("application/json")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Test transaction"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(100.00))
            .andExpect(MockMvcResultMatchers.jsonPath("$.userPerforming").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.sender").value("NL01INHO0000000002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.receiver").value("NL01INHO0000000003"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.transactionType").value("TRANSFER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andReturn();
    }

    @Test
    void getTransactionByIdStatus404TransactionNotFound() throws Exception {
        when(transactionServiceJpa.getTransactionAndValidate(Mockito.anyLong(), Mockito.any(User.class))).thenThrow(new TransactionNotFoundException("Transaction not found"));

        // mock mvc
        // Perform the GET request to fetch the transaction by ID
        mockMvc.perform(get("/transactions/{id}", 1L)
                .header("Authorization", "Bearer " + employeeToken)
                .with(csrf())
                .with(user(authEmployee))
                .contentType("application/json")
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Transaction not found"))
            .andReturn();
    }

    @Test
    void transactionPerformedOnOtherAccountShouldResultIn403Unauthorized() throws Exception {
        when(transactionServiceJpa.postTransaction(Mockito.any(TransactionRequest.class), Mockito.any(User.class))).thenThrow(new UnauthorizedTransactionAccessException("User with id: " + 1 + " is not authorized to access transaction with id: " + 1));

        // mock mvc
        // Perform the POST request to create a transaction
        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + customerToken)
                .with(csrf())
                .with(user(authCustomer))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(transactionRequest))

            )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("User with id: " + 1 + " is not authorized to access transaction with id: " + 1))
            .andReturn();
    }
}
