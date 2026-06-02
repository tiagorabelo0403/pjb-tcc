package com.tcc.pjb.backend.shared.dto;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PJeSubmissaoResponse(
        String numeroProcessoTribunal,
        String protocolo,
        String status,
        Instant submittedAt,
        String correlationId,
        @Schema(description = "Metadados de protocolo de submissao PJe — estrutura definida pelo sistema legado", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadados
) {

    public PJeSubmissaoResponse {
        metadados = metadados == null ? Map.of() : Map.copyOf(metadados);
    }
}

