package com.tcc.pjb.backend.model.dto.governance;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentTrustChainSealRequest(
        @NotNull Long processoId,
        @NotNull UUID documentoId,
        @NotBlank String loteReferencia,
        @NotBlank String motivo,
        boolean contrassinado,
        boolean preservaSigilo,
        String nivelAssinatura
) {
}
