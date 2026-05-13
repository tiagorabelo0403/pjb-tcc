package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record IdentidadeJuridicaAresta(
        String id,
        String origemId,
        String destinoId,
        IdentidadeJuridicaArestaTipo tipo,
        double confianca,
        boolean bidirecional,
        Set<String> fundamentos,
        Map<String, String> atributos
) {
    public IdentidadeJuridicaAresta {
        id = Objects.toString(id, "").trim();
        origemId = Objects.toString(origemId, "").trim();
        destinoId = Objects.toString(destinoId, "").trim();
        tipo = Objects.requireNonNull(tipo, "tipo");
        confianca = clamp(confianca <= 0d ? 1d : confianca);
        fundamentos = sanitizeSet(fundamentos);
        atributos = sanitizeMap(atributos);
        if (id.isBlank() || origemId.isBlank() || destinoId.isBlank()) {
            throw new IllegalArgumentException("id, origemId e destinoId são obrigatórios na aresta");
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
