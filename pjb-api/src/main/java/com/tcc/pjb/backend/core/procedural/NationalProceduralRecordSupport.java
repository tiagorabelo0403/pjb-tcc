package com.tcc.pjb.backend.core.procedural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class NationalProceduralRecordSupport {

    private NationalProceduralRecordSupport() {
    }

    static <T> List<T> copyList(List<T> value) {
        return value == null || value.isEmpty() ? List.of() : List.copyOf(value);
    }

    static Map<String, Object> copyMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        value.forEach((key, entry) -> {
            if (key != null) {
                out.put(key, entry);
            }
        });
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    static <T> List<T> mutableCopy(List<T> value) {
        return value == null || value.isEmpty() ? new ArrayList<>() : new ArrayList<>(value);
    }
}
