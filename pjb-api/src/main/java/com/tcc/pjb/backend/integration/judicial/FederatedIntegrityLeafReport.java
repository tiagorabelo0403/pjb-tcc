package com.tcc.pjb.backend.integration.judicial;

import java.util.LinkedHashMap;
import java.util.Map;

public record FederatedIntegrityLeafReport(
        String key,
        String hash,
        long events,
        boolean changedAgainstPrevious,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("key", key);
        out.put("hash", hash);
        out.put("events", events);
        out.put("changedAgainstPrevious", changedAgainstPrevious);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
