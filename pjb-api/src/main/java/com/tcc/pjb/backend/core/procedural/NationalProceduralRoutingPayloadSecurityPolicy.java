package com.tcc.pjb.backend.core.procedural;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingPayloadSecurityPolicy {

    private static final int MAX_NESTED_DEPTH = 4;
    private static final int MAX_COLLECTION_ENTRIES = 128;

    LinkedHashMap<String, Object> snapshot(Map<String, ?> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (key == null) {
                continue;
            }
            Object sanitized = sanitizeValue(key, entry.getValue(), 0);
            if (sanitized != null) {
                out.put(key, sanitized);
            }
        }
        return out;
    }

    private Object sanitizeValue(String key, Object value, int depth) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?> || value instanceof UUID || value instanceof TemporalAccessor) {
            return value;
        }
        if (value instanceof Character c) {
            return String.valueOf(c);
        }
        if (value instanceof byte[] bytes) {
            return bytes.clone();
        }
        if (depth >= MAX_NESTED_DEPTH) {
            return isInternalKey(key) ? value : null;
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> nestedEntry : map.entrySet()) {
                if (count++ >= MAX_COLLECTION_ENTRIES) {
                    break;
                }
                if (!(nestedEntry.getKey() instanceof String nestedKey)) {
                    continue;
                }
                String normalizedNestedKey = normalizeKey(nestedKey);
                if (normalizedNestedKey == null) {
                    continue;
                }
                Object sanitizedNestedValue = sanitizeValue(normalizedNestedKey, nestedEntry.getValue(), depth + 1);
                if (sanitizedNestedValue != null) {
                    nested.put(normalizedNestedKey, sanitizedNestedValue);
                }
            }
            return nested.isEmpty() ? null : nested;
        }
        if (value instanceof Iterable<?> iterable) {
            ArrayList<Object> nested = new ArrayList<>();
            int count = 0;
            for (Object item : iterable) {
                if (count++ >= MAX_COLLECTION_ENTRIES) {
                    break;
                }
                Object sanitizedItem = sanitizeValue(key, item, depth + 1);
                if (sanitizedItem != null) {
                    nested.add(sanitizedItem);
                }
            }
            return nested.isEmpty() ? null : List.copyOf(nested);
        }
        if (value.getClass().isArray()) {
            int length = Math.min(Array.getLength(value), MAX_COLLECTION_ENTRIES);
            ArrayList<Object> nested = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object sanitizedItem = sanitizeValue(key, Array.get(value, i), depth + 1);
                if (sanitizedItem != null) {
                    nested.add(sanitizedItem);
                }
            }
            return nested.isEmpty() ? null : List.copyOf(nested);
        }
        return isInternalKey(key) ? value : null;
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isInternalKey(String key) {
        return key != null && key.startsWith("__");
    }
}
