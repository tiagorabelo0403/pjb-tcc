package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PeritoHonorariosRequest(
        @PositiveOrZero double valor,
        @NotBlank String justificativa
) {}
