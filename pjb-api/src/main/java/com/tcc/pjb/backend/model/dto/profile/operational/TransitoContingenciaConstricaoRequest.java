package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoContingenciaConstricaoRequest(
        @NotBlank String bem,
        @NotBlank String convenio,
        String statusExterno,
        String referenciaExterna,
        @PositiveOrZero double valorOperacao
) {}
