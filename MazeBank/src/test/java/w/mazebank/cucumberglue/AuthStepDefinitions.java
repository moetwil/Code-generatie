package w.mazebank.cucumberglue;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import w.mazebank.models.requests.RegisterRequest;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class AuthStepDefinitions extends BaseStepDefinitions {
    @When("the client calls endpoint {string}")
    public void clientCallsEndpoints(String endpoint) {
        // Create the request body
        RegisterRequest request = RegisterRequest.builder()
            .email("example@example.com")
            .bsn(123456788)
            .firstName("John")
            .lastName("Doe")
            .password("P@ssw0rd")
            .phoneNumber("1234567890")
            .dateOfBirth(LocalDate.of(1990, 5, 10))
            .build();

        // Create the HTTP entity with the request body and headers
        HttpEntity<RegisterRequest> requestEntity = new HttpEntity<>(request, httpHeaders);
        lastResponse= restTemplate.exchange(
            "http://localhost:" + port + endpoint,
            HttpMethod.POST,
            requestEntity,
            String.class
        );
    }

    @Then("response status code is {int}")
    public void thenStatusCode(int expected){
        assert lastResponse.getStatusCodeValue() == expected;
    }

    @Then("the client should receive a response body matching json:")
    public void thenReturnedStringShouldBe(String expected){
        String resBody = lastResponse.getBody();
        assert resBody != null;
        assert resBody.contains("authenticationToken");
    }
}
