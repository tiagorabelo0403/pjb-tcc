package com.tcc.pjb.backend.integration.judicial;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JudicialMapSupport {

    private JudicialMapSupport() {
    }

    public static Map<String, Object> copyNonNull(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            out.put(entry.getKey(), entry.getValue());
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    public static Map<String, Object> compact(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object rawKey = keyValues[i];
            Object rawValue = keyValues[i + 1];
            if (rawKey == null || rawValue == null) {
                continue;
            }
            String key = String.valueOf(rawKey).trim();
            if (key.isEmpty()) {
                continue;
            }
            out.put(key, rawValue);
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }
}
