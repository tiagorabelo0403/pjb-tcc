package com.tcc.pjb.backend.model.dto.ajuizamento.federal;

import java.util.Map;
import jakarta.validation.constraints.NotBlank;

public record FederalismoEventoRequest(
        @NotBlank String tribunalCodigo,
        @NotBlank String topicKafka,
        @NotBlank String tipoEvento,
        String nupn,
        String operadorId,
        String correlationId,
        String idempotencyKey,
        Long schemaVersion,
        Integer prioridade,
        Boolean validarAssinatura,
        String payload,
        Map<String, Object> metadata
) {
}
