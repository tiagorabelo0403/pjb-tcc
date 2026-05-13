package com.tcc.pjb.backend.core.identidade.vinculo.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record IdentidadeJuridicaVinculoParte(
        IdentidadeJuridicaPapelProcessual papel,
        String nome,
        String documento,
        String email,
        String telefone,
        String numeroOab,
        String polo,
        Map<String, String> atributos
) {
    public IdentidadeJuridicaVinculoParte {
        papel = Objects.requireNonNull(papel, "papel");
        nome = normalizeNullable(nome);
        documento = normalizeNullable(documento);
        email = normalizeNullable(email);
        telefone = normalizeNullable(telefone);
        numeroOab = normalizeNullable(numeroOab);
        polo = normalizeNullable(polo);
        atributos = sanitize(atributos);
        if (nome == null && documento == null && email == null && numeroOab == null) {
            throw new IllegalArgumentException("a parte vinculada exige ao menos um identificador");
        }
    }

    private static Map<String, String> sanitize(Map<String, String> source) {
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
