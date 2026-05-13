package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoReconciliacaoConstricaoRequest(
        @NotBlank String bem,
        String convenio,
        @NotBlank String statusExterno,
        String referenciaExterna,
        @PositiveOrZero double valorOperacao
) {}
