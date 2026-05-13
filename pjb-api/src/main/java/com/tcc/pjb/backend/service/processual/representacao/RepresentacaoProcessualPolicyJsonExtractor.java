package com.tcc.pjb.backend.service.processual.representacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RepresentacaoProcessualPolicyJsonExtractor {

    private RepresentacaoProcessualPolicyJsonExtractor() {
    }

    public static Map<String, Object> extract(ObjectMapper objectMapper, String poderesJson) {
        if (objectMapper == null || poderesJson == null || poderesJson.isBlank()) {
            return Map.of();
        }
        try {
            Object parsed = objectMapper.readValue(poderesJson, Object.class);
            if (!(parsed instanceof Map<?, ?> root)) {
                return Map.of();
            }
            Object policy = root.get("representationPolicy");
            if (!(policy instanceof Map<?, ?> policyMap)) {
                return Map.of();
            }
            LinkedHashMap<String, Object> extracted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : policyMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String key = String.valueOf(entry.getKey()).trim();
                if (key.isBlank()) {
                    continue;
                }
                extracted.put(key, normalize(entry.getValue()));
            }
            return extracted.isEmpty() ? Map.of() : Map.copyOf(extracted);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String key = String.valueOf(entry.getKey()).trim();
                if (key.isBlank()) {
                    continue;
                }
                normalized.put(key, normalize(entry.getValue()));
            }
            return normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.ArrayList<Object> out = new java.util.ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    out.add(normalize(item));
                }
            }
            return java.util.List.copyOf(out);
        }
        if (value instanceof CharSequence text) {
            String normalized = text.toString().trim();
            return normalized.isEmpty() ? null : normalized;
        }
        return value;
    }

    public static boolean regularidadeSuficiente(ObjectMapper objectMapper, String poderesJson) {
        Map<String, Object> policy = extract(objectMapper, poderesJson);
        Object value = policy.get("regularidadeSuficiente");
        return !Boolean.FALSE.equals(value);
    }

    public static boolean exigeTermoOuAta(ObjectMapper objectMapper, String poderesJson) {
        Map<String, Object> policy = extract(objectMapper, poderesJson);
        return Boolean.TRUE.equals(policy.get("exigeTermoOuAtaAudiencia"));
    }

    public static String firstAlerta(ObjectMapper objectMapper, String poderesJson, String fallback) {
        Map<String, Object> policy = extract(objectMapper, poderesJson);
        Object alertas = policy.get("alertas");
        if (alertas instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String text = Objects.toString(item, "").trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return fallback;
    }

    public static boolean hasReference(ObjectMapper objectMapper, String poderesJson, String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        Object value = extract(objectMapper, poderesJson).get(key.trim());
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim();
        return !text.isBlank() && !"null".equalsIgnoreCase(text);
    }
}
