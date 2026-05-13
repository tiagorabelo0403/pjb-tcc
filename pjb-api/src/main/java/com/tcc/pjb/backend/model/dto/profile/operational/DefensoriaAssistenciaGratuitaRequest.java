package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record DefensoriaAssistenciaGratuitaRequest(
        @NotBlank String renda,
        @NotBlank String justificativa
) {}
