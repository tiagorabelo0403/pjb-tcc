package com.tcc.pjb.backend.ai.juridica.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JudexGenerateMinutaRequest(
        @Positive Long processoId,
        @Size(max = 8000) String promptAdicional,
        @Size(max = 120000) String analiseV1,
        @Size(max = 120000) String peticaoInicialText
) {
}
