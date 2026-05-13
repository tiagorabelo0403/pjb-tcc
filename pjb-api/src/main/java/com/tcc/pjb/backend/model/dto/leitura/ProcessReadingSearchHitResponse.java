package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ProcessReadingSearchHitResponse(
        UUID documentoId,
        String pageId,
        int pageNumber,
        String tituloDocumento,
        String fragment,
        String lane,
        String endpoint,
        String sourceType,
        String sourceId,
        String sourceLabel,
        Map<String, Object> metadata
) {
    public ProcessReadingSearchHitResponse {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
