package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record DiligenceCertificateResponse(
        Long certidaoId,
        String actor,
        String canal,
        String diligenciaReferencia,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long checkpointEventId,
        String certidaoTipo,
        String titulo,
        String narrativa,
        String certificateDigestSha256,
        String signatureHmacSha256,
        Double latitude,
        Double longitude,
        Double destinoLatitude,
        Double destinoLongitude,
        Double distanceMeters,
        Boolean insideGeofence,
        Integer tentativaSequencia,
        String evidenceChaveCustodia,
        String attemptTrailDigestSha256,
        Instant createdAt,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
    public DiligenceCertificateResponse {
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
