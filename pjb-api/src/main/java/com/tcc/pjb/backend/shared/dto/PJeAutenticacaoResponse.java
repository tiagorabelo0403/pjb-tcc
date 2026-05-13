package com.tcc.pjb.backend.shared.dto;

import java.time.Instant;
import java.util.Map;

public record PJeAutenticacaoResponse(
        String token,
        Instant expiresAt,
        String tribunal,
        String correlationId,
        Map<String, Object> metadados
) {

    public PJeAutenticacaoResponse {
        metadados = metadados == null ? Map.of() : Map.copyOf(metadados);
    }

    public boolean isValido() {
        return token != null && !token.isBlank() && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
