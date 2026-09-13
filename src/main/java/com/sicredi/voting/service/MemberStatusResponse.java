package com.sicredi.voting.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload retornado pelo serviço externo {@code GET /users/{cpf}}:
 * {@code {"status": "ABLE_TO_VOTE"}} ou {@code {"status": "UNABLE_TO_VOTE"}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberStatusResponse(String status) {

    public static final String ABLE_TO_VOTE = "ABLE_TO_VOTE";

    public boolean canVote() {
        return ABLE_TO_VOTE.equals(status);
    }
}