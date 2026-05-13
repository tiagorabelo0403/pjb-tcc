package com.tcc.pjb.backend.model.dto.transito;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostArchiveAccessRequest(
        @NotNull Long processoId,
        @Size(max = 1000) String motivo,
        boolean solicitarCopiaIntegral,
        boolean solicitarReativacaoControlada
) {
}
