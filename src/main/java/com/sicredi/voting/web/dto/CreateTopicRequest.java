package com.sicredi.voting.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @NotBlank(message = "title é obrigatório")
        @Size(max = 120, message = "title deve ter no máximo 120 caracteres")
        String title,

        @Size(max = 500, message = "description deve ter no máximo 500 caracteres")
        String description
) {}