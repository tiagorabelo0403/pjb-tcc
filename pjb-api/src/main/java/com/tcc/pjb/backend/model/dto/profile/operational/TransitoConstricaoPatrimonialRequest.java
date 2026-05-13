package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoConstricaoPatrimonialRequest(
        @NotBlank String ato,
        @NotBlank String bem,
        String detalhe,
        String convenio,
        @PositiveOrZero double valorOperacao
) {}
