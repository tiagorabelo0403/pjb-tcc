package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoIncidenteExecutivoRequest(
        @NotBlank String incidente,
        String fundamentacao,
        @PositiveOrZero double valorGarantia
) {}
