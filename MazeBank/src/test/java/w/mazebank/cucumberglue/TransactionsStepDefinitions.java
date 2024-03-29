package w.mazebank.cucumberglue;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import w.mazebank.enums.AccountType;
import w.mazebank.models.Account;
import w.mazebank.models.requests.TransactionRequest;
import w.mazebank.utils.IbanGenerator;

import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransactionsStepDefinitions extends BaseStepDefinitions {
    private Account savingsAccount;
    private Account checkingsAccount;

    @When("I call the transactions endpoint with a {string} and {string} parameter")
    public void iCallTheTransactionsEndpointWithStartDateAndEndDateParameter(String startDate, String endDate) {

        httpHeaders.clear();
        token = jwtService.generateToken(customer);
        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(null, httpHeaders);

        // Send the request
        lastResponse = restTemplate.exchange(
            "http://localhost:" + port + "/users/3/transactions?startDate=" + startDate + "&endDate=" + endDate,
            HttpMethod.GET, // Adjust the HTTP method if necessary
            requestEntity,
            String.class
        );
    }

    @Then("I should see a list of transactions within the specified date range")
    public void iShouldSeeAListOfTransactionsWithinTheSpecifiedDateRange() {

        // assert that the response body is not null
        assert lastResponse.getBody() != null;

        System.out.println(lastResponse.getBody());
    }

    @When("I call the users endpoint with a search {string} parameter")
    public void iCallTheAccountsEndpointWithAnIBANParameter(String search) {
        httpHeaders.clear();
        token = jwtService.generateToken(customer);
        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(null, httpHeaders);

        // Send the request
        lastResponse = restTemplate.exchange(
            "http://localhost:" + port + "/users/3/transactions?" + search,
            HttpMethod.GET, // Adjust the HTTP method if necessary
            requestEntity,
            String.class
        );
    }

    @Then("I should see a list of transactions with the IBAN parameter")
    public void iShouldSeeAListOfTransactionsWithTheIBAN() {
        assert lastResponse.getBody() != null;
        System.out.println(lastResponse.getBody());
    }

    @And("I have a savings account with balance {double}")
    public void iHaveASavingsAccountWithBalance(double balance) {

        // create a savings account
        savingsAccount = Account.builder()
            .id(1)
            .iban(IbanGenerator.generate())
            .accountType(AccountType.SAVINGS)
            .balance(1000.00)
            .build();

        // add the savings account to the customer
        customer.setAccounts(List.of(savingsAccount));
    }

    @When("I make a transaction from my savings account to customer {int}")
    public void iMakeATransactionFromMySavingsAccountToCustomer(int receivingUserId) {
        // create body for transactionRequest
        TransactionRequest transactionRequest = TransactionRequest.builder()
            .amount(100.00)
            .receiverIban("NL76INHO0493458014")
            .senderIban("NL76INHO0493458015")
            .build();

        httpHeaders.clear();

        token = jwtService.generateToken(customer);

        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(transactionRequest, httpHeaders);

        try {
            // Send the request
            lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/transactions",
                HttpMethod.POST,
                requestEntity,
                String.class
            );
        } catch (HttpClientErrorException.BadRequest ex) {
            // pass the response to the lastResponse variable
            lastResponse = new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getRawStatusCode());
        }
    }

    @Then("response status code is {int} with message {string}")
    public void responseStatusCodeIsWithMessage(int statusCode, String errorMessage) {
        // Assert the response status code
        assertEquals(statusCode, lastResponse.getStatusCode().value());
        System.out.println(lastResponse.getBody());

        // Assert the response body
        assertTrue(lastResponse.getBody().contains(errorMessage));
    }

    @When("I make a transaction from customer {int}  to a savings account")
    public void iMakeATransactionFromCustomerToMySavingsAccount(int arg0) {
        // create body for transactionRequest
        TransactionRequest transactionRequest = TransactionRequest.builder()
            .amount(100.00)
            .senderIban("NL76INHO0493458014")
            .receiverIban("NL76INHO0493458015")
            .build();

        httpHeaders.clear();

        token = jwtService.generateToken(customer);

        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(transactionRequest, httpHeaders);

        try {
            // Send the request
            lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/transactions",
                HttpMethod.POST,
                requestEntity,
                String.class
            );
        } catch (HttpClientErrorException.BadRequest ex) {
            // pass the response to the lastResponse variable
            lastResponse = new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getRawStatusCode());
        }
    }

    @When("I make a transaction from account {int} to account {int}")
    public void iMakeATransactionFromAccountToAccount(int accountIdSender, int accountIdReceiver) {
        TransactionRequest transactionRequest = TransactionRequest.builder()
            .amount(100.00)
            .senderIban("NL76INHO0493458014")
            .receiverIban("NL76INHO0493458018")
            .build();

        httpHeaders.clear();

        token = jwtService.generateToken(employee);

        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(transactionRequest, httpHeaders);

        try {
            // Send the request
            lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/transactions",
                HttpMethod.POST,
                requestEntity,
                String.class
            );
        } catch (HttpClientErrorException.BadRequest ex) {
            // pass the response to the lastResponse variable
            lastResponse = new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getRawStatusCode());
        }
    }

    @Then("the result is a {int} status code. and a transaction with id {int}, amount {double}, from account with iban {string} to account with iban {string}")
    public void theResultIsAStatusCodeAndATransactionWithIdAmountFromAccountWithIbanNLINHOToAccountWithIbanNLINHO(int statusCode, int transactionId, double amount, String senderIban, String receiverIban) {
        // Assert the response status code
        assertEquals(statusCode, lastResponse.getStatusCodeValue());

        System.out.println(lastResponse.getBody());

        // Assert the response body
        assertTrue(Objects.requireNonNull(lastResponse.getBody()).contains("id"));
        assertTrue(lastResponse.getBody().contains("amount"));
        assertTrue(lastResponse.getBody().contains("sender"));
        assertTrue(lastResponse.getBody().contains("receiver"));

        // Assert the response body
        assertTrue(lastResponse.getBody().contains(String.valueOf(transactionId)));
        assertTrue(lastResponse.getBody().contains(String.valueOf(amount)));
        assertTrue(lastResponse.getBody().contains(senderIban));
        assertTrue(lastResponse.getBody().contains(receiverIban));
    }


    @When("I call the users endpoint with a parameter that asks for transactions less than {int} amount")
    public void iCallTheUsersEndpointWithAParameterThatAsksForTransactionsLessThanAmount(int amount) {
        httpHeaders.clear();

        token = jwtService.generateToken(employee);

        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(null, httpHeaders);

        // Send the request
        lastResponse = restTemplate.exchange(
            "http://localhost:" + port + "/users/3/transactions?maxAmount=" + amount,
            HttpMethod.GET, // Adjust the HTTP method if necessary
            requestEntity,
            String.class
        );
    }


    @Then("I should see a list of transactions with transactions less than given amount")
    public void iShouldSeeAListOfTransactionsWithTransactionsLessThanAmount() {
        assert lastResponse.getBody() != null;
        System.out.println(lastResponse.getBody());
    }

    @When("I call the users endpoint with a parameter that asks for transactions equal to {int} amount")
    public void iCallTheUsersEndpointWithAParameterThatAsksForTransactionsEqualToAmount(int amount) {
        httpHeaders.clear();

        token = jwtService.generateToken(employee);

        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(null, httpHeaders);

        // Send the request
        lastResponse = restTemplate.exchange(
            "http://localhost:" + port + "/users/3/transactions?amount=" + amount,
            HttpMethod.GET, // Adjust the HTTP method if necessary
            requestEntity,
            String.class
        );
    }

    @Then("I should see a list of transactions with balances equal to given amount")
    public void iShouldSeeAListOfTransactionsWithBalancesEqualToGivenAmount() {
        assert lastResponse.getBody() != null;
        System.out.println(lastResponse.getBody());
    }

    @When("I call the users endpoint with a parameter that asks for transactions more than {int} amount")
    public void iCallTheUsersEndpointWithAParameterThatAsksForTransactionsMoreThanAmount(int amount) {
        httpHeaders.clear();

        token = jwtService.generateToken(employee);

        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(null, httpHeaders);

        // Send the request
        lastResponse = restTemplate.exchange(
            "http://localhost:" + port + "/users/3/transactions?minAmount=" + amount,
            HttpMethod.GET, // Adjust the HTTP method if necessary
            requestEntity,
            String.class
        );
    }

    @Then("I should see a list of transactions with balances greater than given amount")
    public void iShouldSeeAListOfTransactionsWithBalancesGreaterThanGivenAmount() {
        assert lastResponse.getBody() != null;
        System.out.println(lastResponse.getBody());
    }


    @Then("the result is a successful transaction with given fields")
    public void theResultIsASuccessfulTransactionWithGivenFields() {
        assert lastResponse.getBody() != null;
        System.out.println(lastResponse.getBody());
    }

    @And("I have an account with iban {string} and balance {double} and absoluteLimit {double}")
    public void iHaveAnAccountWithIbanAndBalanceAndAbsoluteLimit(String iban, double balance, double absoluteLimit) {
        checkingsAccount = Account.builder()
            .id(8L)
            .iban(iban)
            .accountType(AccountType.CHECKING)
            .balance(balance)
            .absoluteLimit(absoluteLimit)
            .isActive(true)
            .build();
        customer.setAccounts(List.of(checkingsAccount));
    }

    @And("I have a user with transactionLimit {double}")
    public void iHaveAUserWithTransactionLimit(double transactionLimit) {
        customer.setTransactionLimit(transactionLimit);
    }

    @When("I make a transaction from account with iban {string} to account with iban {string} with amount {double}")
    public void iMakeATransactionFromAccountToAccountWithAmount(String senderIban, String receiverIban, double amount) {
        token = jwtService.generateToken(customer);

        TransactionRequest transactionRequest = TransactionRequest.builder()
            .senderIban(senderIban)
            .receiverIban(receiverIban)
            .amount(amount)
            .build();

        // System.out.println(customer.getTransactionLimit());
        // System.out.println(customer.getAccounts().get(0).getAbsoluteLimit());
        // System.out.println(customer.getAccounts().get(0).getBalance());

        // call the endpoint /accounts with a POST request
        httpHeaders.clear();
        httpHeaders.add("Authorization", "Bearer " + token);

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(transactionRequest, httpHeaders);

        try {
            lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/transactions",
                HttpMethod.POST,
                requestEntity,
                String.class
            );
        } catch (HttpClientErrorException.BadRequest ex) {
            // pass the response to the lastResponse variable
            lastResponse = new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode().value());
        }
    }
}
