package com.tcc.pjb.backend.model.dto.notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record IntimacaoMulticanalDispatchResponse(Long processoId,
                                                  Long usuarioId,
                                                  List<String> deliveredChannels,
                                                  List<String> suppressedChannels,
                                                  List<String> failedChannels,
                                                  List<String> trackingTokens,
                                                  String status,
                                                  Instant generatedAt,
                                                  List<String> rationale,
                                                  Map<String, Object> documentoFormalAssinado,
                                                  Map<String, Object> assinaturaQualificada,
                                                  Map<String, Object> validacaoSoberana) {
    public IntimacaoMulticanalDispatchResponse {
        documentoFormalAssinado = immutableMap(documentoFormalAssinado);
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
