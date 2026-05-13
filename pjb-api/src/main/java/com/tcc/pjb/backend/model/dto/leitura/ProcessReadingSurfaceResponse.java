package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingSurfaceResponse(
        String readerType,
        String sourceType,
        String displayMode,
        String extractionMode,
        String selectionMode,
        String ocrStatus,
        String preservationMode,
        String timelineMode,
        String contentEndpoint,
        String pdfEndpoint,
        String downloadEndpoint,
        List<String> markers,
        Map<String, Object> metadata
) {
    public ProcessReadingSurfaceResponse {
        markers = markers == null ? List.of() : List.copyOf(markers);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
