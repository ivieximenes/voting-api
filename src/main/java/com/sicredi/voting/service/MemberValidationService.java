package com.sicredi.voting.service;

import com.sicredi.voting.util.CpfValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberValidationService {

    private final RestClient restClient;
    private final boolean enabled;

    public MemberValidationService(
            @Qualifier("memberValidationRestClient") RestClient restClient,
            @Value("${sicredi.member-validation.enabled:true}") boolean enabled) {
        this.restClient = restClient;
        this.enabled = enabled;
    }

    public void validate(String memberId) {
        if (!CpfValidator.isValid(memberId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CPF inválido: " + memberId);
        }

        // Toggle para desligar a chamada ao serviço externo (ex: testes, ambiente sem rede),
        // sem precisar mockar o RestClient inteiro.
        if (!enabled) {
            return;
        }

        try {
            MemberStatusResponse response = restClient.get()
                    .uri("/users/{cpf}", memberId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, res) -> {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "CPF não encontrado no serviço de validação: " + memberId);
                    })
                    .body(MemberStatusResponse.class);

            if (response == null || response.status() == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Resposta inválida do serviço de validação de associado");
            }
            if (!response.canVote()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Associado não elegível para votar: " + memberId);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            // Timeout, DNS, conexão recusada etc: falha do serviço externo, não do cliente.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de validação de associado indisponível no momento",
                    e);
        }
    }
}