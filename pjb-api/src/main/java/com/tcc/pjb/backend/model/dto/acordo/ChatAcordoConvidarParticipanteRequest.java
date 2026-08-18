package com.tcc.pjb.backend.model.dto.acordo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatAcordoConvidarParticipanteRequest(
        @NotNull @Positive Long usuarioId,
        String papel
) {
}
