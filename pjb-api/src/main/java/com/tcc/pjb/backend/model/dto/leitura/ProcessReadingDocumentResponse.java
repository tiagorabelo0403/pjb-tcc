package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProcessReadingDocumentResponse(
        UUID documentoId,
        String titulo,
        String categoria,
        String contentType,
        long tamanhoBytes,
        long totalPaginas,
        int coberturaTextualPercentual,
        String suggestedMode,
        List<String> markers,
        Map<String, Object> metadata
) {
    public ProcessReadingDocumentResponse {
        markers = markers == null ? List.of() : List.copyOf(markers);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
