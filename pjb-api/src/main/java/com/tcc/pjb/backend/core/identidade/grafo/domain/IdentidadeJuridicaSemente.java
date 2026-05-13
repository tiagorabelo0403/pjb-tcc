package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record IdentidadeJuridicaSemente(
        IdentidadeJuridicaChaveTipo tipo,
        String valor,
        String rotulo,
        String polo,
        Map<String, String> atributos
) {
    public IdentidadeJuridicaSemente {
        tipo = Objects.requireNonNull(tipo, "tipo");
        valor = Objects.toString(valor, "").trim();
        rotulo = Objects.toString(rotulo, valor).trim();
        polo = normalizeNullable(polo);
        atributos = sanitize(atributos);
        if (valor.isBlank()) {
            throw new IllegalArgumentException("valor da semente é obrigatório");
        }
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static Map<String, String> sanitize(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = normalizeNullable(key);
            String normalizedValue = normalizeNullable(value);
            if (normalizedKey != null && normalizedValue != null) {
                sanitized.put(normalizedKey, normalizedValue);
            }
        });
        return Map.copyOf(sanitized);
    }
}
