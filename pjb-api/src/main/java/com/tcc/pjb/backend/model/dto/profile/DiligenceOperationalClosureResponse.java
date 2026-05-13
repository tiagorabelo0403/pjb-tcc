package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record DiligenceOperationalClosureResponse(
        Long encerramentoId,
        String actor,
        String canal,
        String diligenciaReferencia,
        String outcome,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long certidaoId,
        Long checkpointEventId,
        String certidaoDigestSha256,
        String workItemStatusFinal,
        Long followupWorkItemId,
        Integer documentosVinculados,
        String idempotencyKey,
        String executionDigestSha256,
        Instant createdAt,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
    public DiligenceOperationalClosureResponse {
        assinaturaQualificada = immutableMap(assinaturaQualificada);
        validacaoSoberana = immutableMap(validacaoSoberana);
    }

    private static Map<String, Object> immutableMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }
}
