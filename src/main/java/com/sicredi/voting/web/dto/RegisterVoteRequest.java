package com.sicredi.voting.web.dto;

import com.sicredi.voting.enums.VoteOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterVoteRequest(

        @NotBlank(message = "O CPF é obrigatório")
        @Size(min = 11, max = 11, message = "O CPF deve ter 11 dígitos")
        String memberId,

        @NotNull(message = "A opção de voto é obrigatória")
        VoteOption option
) {
}
