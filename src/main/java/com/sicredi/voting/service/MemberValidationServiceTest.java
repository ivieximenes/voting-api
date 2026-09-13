package com.sicredi.voting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MemberValidationServiceTest {

    private static final String VALID_CPF = "11144477735";
    private static final String BASE_URL = "http://fake-user-info";

    private MockRestServiceServer mockServer;
    private MemberValidationService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new MemberValidationService(builder.build(), true);
    }

    @Test
    void shouldThrowBadRequestWhenCpfHasInvalidFormat() {
        assertThatThrownBy(() -> service.validate("123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldValidateSuccessfullyWhenMemberCanVote() {
        mockServer.expect(requestTo(BASE_URL + "/users/" + VALID_CPF))
                .andRespond(withSuccess("{\"status\":\"ABLE_TO_VOTE\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> service.validate(VALID_CPF)).doesNotThrowAnyException();
        mockServer.verify();
    }

    @Test
    void shouldThrowUnprocessableEntityWhenMemberCannotVote() {
        mockServer.expect(requestTo(BASE_URL + "/users/" + VALID_CPF))
                .andRespond(withSuccess("{\"status\":\"UNABLE_TO_VOTE\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.validate(VALID_CPF))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void shouldThrowNotFoundWhenServiceReturns404() {
        mockServer.expect(requestTo(BASE_URL + "/users/" + VALID_CPF))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.validate(VALID_CPF))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldThrowServiceUnavailableWhenExternalServiceFails() {
        mockServer.expect(requestTo(BASE_URL + "/users/" + VALID_CPF))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> service.validate(VALID_CPF))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void shouldNotCallExternalServiceWhenIntegrationIsDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer verifyServer = MockRestServiceServer.bindTo(builder).build();
        MemberValidationService disabled = new MemberValidationService(builder.build(), false);

        assertThatCode(() -> disabled.validate(VALID_CPF)).doesNotThrowAnyException();
        verifyServer.verify();
    }
}