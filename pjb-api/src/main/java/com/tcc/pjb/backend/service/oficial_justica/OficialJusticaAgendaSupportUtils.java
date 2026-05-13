package com.tcc.pjb.backend.service.oficial_justica;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class OficialJusticaAgendaSupportUtils {

    private OficialJusticaAgendaSupportUtils() {
    }

    static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }

    static Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }

    static List<String> unique(List<String> values) {
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }

    static List<String> nonBlank(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).toList();
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    static Long parseId(String raw) {
        try {
            return raw == null ? null : Long.parseLong(raw);
        } catch (Exception ex) {
            return null;
        }
    }
}
