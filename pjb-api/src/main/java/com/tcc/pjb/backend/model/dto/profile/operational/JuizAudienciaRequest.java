package com.tcc.pjb.backend.model.dto.profile.operational;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JuizAudienciaRequest(
        @NotNull Instant dataHora,
        @NotBlank String tipo,
        @NotBlank String local
) {}
