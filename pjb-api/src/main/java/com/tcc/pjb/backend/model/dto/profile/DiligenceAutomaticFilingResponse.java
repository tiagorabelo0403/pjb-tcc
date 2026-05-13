package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record DiligenceAutomaticFilingResponse(
        Long juntadaId,
        String actor,
        String canal,
        String diligenciaReferencia,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long formalizacaoId,
        Long encerramentoId,
        Long certidaoId,
        UUID minutaDocumentoId,
        UUID pacoteDocumentoId,
        Long movimentacaoId,
        Long movimentacaoEventSeq,
        Long pacoteEventSeq,
        String evidenceChaveCustodia,
        Boolean evidenceIntegrityOk,
        Integer documentosReferenciados,
        String externalSystemCode,
        String bundleReference,
        String bundleDigestSha256,
        String bundleSignatureHmacSha256,
        String idempotencyKey,
        Instant createdAt,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
    public DiligenceAutomaticFilingResponse {
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
