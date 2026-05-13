package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoIntegracaoConstricaoRequest(
        @NotBlank String ato,
        @NotBlank String bem,
        String convenio,
        String referenciaExterna,
        @PositiveOrZero double valorOperacao
) {}
