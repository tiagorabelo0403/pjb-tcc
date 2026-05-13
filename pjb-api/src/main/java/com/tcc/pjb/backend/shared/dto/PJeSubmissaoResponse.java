package com.tcc.pjb.backend.shared.dto;

import java.time.Instant;
import java.util.Map;

public record PJeSubmissaoResponse(
        String numeroProcessoTribunal,
        String protocolo,
        String status,
        Instant submittedAt,
        String correlationId,
        Map<String, Object> metadados
) {

    public PJeSubmissaoResponse {
        metadados = metadados == null ? Map.of() : Map.copyOf(metadados);
    }
}
