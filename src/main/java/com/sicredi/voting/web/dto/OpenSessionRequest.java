package com.sicredi.voting.web.dto;

import jakarta.validation.constraints.Positive;

public record OpenSessionRequest(
        @Positive(message = "A duração deve ser um número positivo")
        Integer durationSeconds
) {
}
