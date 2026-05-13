package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoCicloLeilaoExpropriatorioRequest(
        @NotBlank String ato,
        @NotBlank String bem,
        String modalidade,
        @Min(1) int tentativa,
        @PositiveOrZero double valorReferencia
) {}
