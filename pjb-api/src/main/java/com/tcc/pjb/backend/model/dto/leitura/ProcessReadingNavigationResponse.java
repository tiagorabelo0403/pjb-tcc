package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingNavigationResponse(
        Long processoId,
        String navigationMode,
        String chronologyMode,
        int totalNodes,
        List<ProcessReadingNavigationNodeResponse> nodes,
        Map<String, Object> metadata
) {
    public ProcessReadingNavigationResponse {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
