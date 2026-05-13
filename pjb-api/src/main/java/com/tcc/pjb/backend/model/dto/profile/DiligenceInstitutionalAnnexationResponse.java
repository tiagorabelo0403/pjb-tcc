package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record DiligenceInstitutionalAnnexationResponse(
        Long annexationId,
        String perfil,
        String canal,
        String diligenceReference,
        Long processoId,
        String processoNumero,
        Long juntadaId,
        Long formalizacaoId,
        Long encerramentoId,
        Long certidaoId,
        UUID pacoteDocumentoId,
        String externalSystemCode,
        String destinationBox,
        String ackProtocol,
        String ackReference,
        String annexationStatus,
        String bundleReference,
        String chainIdempotencyKey,
        String executionDigestSha256,
        Long processEventSeq,
        OffsetDateTime externalizedAt,
        Instant createdAt,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
    public DiligenceInstitutionalAnnexationResponse {
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
