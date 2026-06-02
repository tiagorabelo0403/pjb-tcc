package com.tcc.pjb.backend.shared.dto;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PJeAutenticacaoResponse(
        String token,
        Instant expiresAt,
        String tribunal,
        String correlationId,
        @Schema(description = "Metadados de autenticacao SSO do PJe — estrutura definida pelo sistema legado", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadados
) {

    public PJeAutenticacaoResponse {
        metadados = metadados == null ? Map.of() : Map.copyOf(metadados);
    }

    public boolean isValido() {
        return token != null && !token.isBlank() && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}

