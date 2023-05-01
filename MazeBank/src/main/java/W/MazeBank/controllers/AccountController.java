package w.mazebank.controllers;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import w.mazebank.exceptions.AccountsNotFoundException;
import w.mazebank.exceptions.UnauthorizedAccountAccessException;
import w.mazebank.exceptions.UserNotFoundException;
import w.mazebank.models.Account;
import w.mazebank.models.Transaction;
import w.mazebank.models.requests.DepositRequest;
import w.mazebank.models.responses.TransactionResponse;
import w.mazebank.services.AccountServiceJpa;
import w.mazebank.utils.ResponseHandler;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountServiceJpa accountServiceJpa;


    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
        @PathVariable("accountId") Long accountId,
        @RequestBody DepositRequest depositRequest,
        @AuthenticationPrincipal UserDetails userDetails) throws AccountsNotFoundException {

        // create deposit transaction
        Transaction transaction = accountServiceJpa.deposit(accountId, depositRequest.getAmount(), userDetails);

        // create transaction response and return it
        TransactionResponse transactionResponse = TransactionResponse.builder()
            .message("Deposit successful")
            .build();
        return ResponseEntity.ok(transactionResponse);
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<Object> blockUser(@PathVariable Long id) throws AccountsNotFoundException {
        accountServiceJpa.lockAccount(id);
        return ResponseEntity.ok(ResponseHandler.generateResponse("Account with id: " + id + " locked"));
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<Object> unblockUser(@PathVariable Long id) throws AccountsNotFoundException {
        accountServiceJpa.unlockAccount(id);
        return ResponseEntity.ok(ResponseHandler.generateResponse("Account with id: " + id + " unlocked"));
    }
}
