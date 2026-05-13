package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PjbSubstituicaoPayloadSupport {

    private PjbSubstituicaoPayloadSupport() {
    }

    static Map<String, Object> immutableMap(Map<?, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                out.put(String.valueOf(key), immutableValue(value));
            }
        });
        return java.util.Collections.unmodifiableMap(out);
    }

    static List<Object> immutableList(List<?> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        ArrayList<Object> out = new ArrayList<>(source.size());
        for (Object value : source) {
            out.add(immutableValue(value));
        }
        return java.util.Collections.unmodifiableList(out);
    }

    static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return immutableMap(map);
        }
        if (value instanceof List<?> list) {
            return immutableList(list);
        }
        return value;
    }
}
