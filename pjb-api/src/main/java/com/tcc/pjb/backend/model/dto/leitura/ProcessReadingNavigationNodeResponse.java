package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ProcessReadingNavigationNodeResponse(
        UUID documentoId,
        String pageId,
        int pageNumber,
        String nodeType,
        String label,
        int priority,
        String fragment,
        String endpoint,
        Map<String, Object> metadata
) {
    public ProcessReadingNavigationNodeResponse {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
