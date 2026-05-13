package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record IdentidadeJuridicaVertice(
        String id,
        IdentidadeJuridicaVerticeTipo tipo,
        String chaveCanonica,
        String rotulo,
        double confianca,
        Set<String> fontes,
        Map<String, String> atributos
) {
    public IdentidadeJuridicaVertice {
        id = Objects.toString(id, "").trim();
        tipo = Objects.requireNonNull(tipo, "tipo");
        chaveCanonica = Objects.toString(chaveCanonica, "").trim();
        rotulo = Objects.toString(rotulo, chaveCanonica).trim();
        confianca = clamp(confianca <= 0d ? 1d : confianca);
        fontes = sanitizeSet(fontes);
        atributos = sanitizeMap(atributos);
        if (id.isBlank()) {
            throw new IllegalArgumentException("id do vértice é obrigatório");
        }
        if (chaveCanonica.isBlank()) {
            throw new IllegalArgumentException("chave canônica do vértice é obrigatória");
        }
    }

    private static double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private static Set<String> sanitizeSet(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : source) {
            String normalized = normalizeNullable(item);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return Set.copyOf(values);
    }

    private static Map<String, String> sanitizeMap(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = normalizeNullable(key);
            String normalizedValue = normalizeNullable(value);
            if (normalizedKey != null && normalizedValue != null) {
                values.put(normalizedKey, normalizedValue);
            }
        });
        return Map.copyOf(values);
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
