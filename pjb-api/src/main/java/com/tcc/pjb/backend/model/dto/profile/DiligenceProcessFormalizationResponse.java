package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record DiligenceProcessFormalizationResponse(
        Long formalizacaoId,
        String actor,
        String canal,
        String diligenciaReferencia,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long encerramentoId,
        Long certidaoId,
        Long checkpointEventId,
        Long movimentacaoId,
        Long movimentacaoEventSeq,
        UUID minutaDocumentoId,
        Long minutaEventSeq,
        String minutaTitulo,
        String minutaSha256,
        String certidaoDigestSha256,
        String evidenceChaveCustodia,
        Boolean evidenceIntegrityOk,
        Integer documentosReferenciados,
        String idempotencyKey,
        String formalizationDigestSha256,
        Instant createdAt,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
    public DiligenceProcessFormalizationResponse {
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
