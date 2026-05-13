package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoGovernancaExpropriacaoRequest(
        @NotBlank String ato,
        @NotBlank String bem,
        String modalidade,
        @PositiveOrZero double valorReferencia
) {}
