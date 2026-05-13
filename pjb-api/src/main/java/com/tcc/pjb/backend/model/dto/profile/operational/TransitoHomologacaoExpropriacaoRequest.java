package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoHomologacaoExpropriacaoRequest(
        @NotBlank String ato,
        @NotBlank String bem,
        String modalidade,
        String adquirente,
        @PositiveOrZero double valorArrematacao
) {}
