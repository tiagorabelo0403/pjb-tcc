package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingFlowResponse(
        long totalEntries,
        long totalInlineActs,
        long totalMovements,
        long totalEvents,
        String chronologyMode,
        String defaultOpenMode,
        List<ProcessReadingProcessEntryResponse> entries,
        Map<String, Object> metadata
) {
    public ProcessReadingFlowResponse {
        entries = entries == null ? List.of() : List.copyOf(entries);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
