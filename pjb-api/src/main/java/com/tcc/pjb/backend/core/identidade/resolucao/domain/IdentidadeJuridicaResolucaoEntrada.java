package com.tcc.pjb.backend.core.identidade.resolucao.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record IdentidadeJuridicaResolucaoEntrada(
        String origem,
        String nome,
        String documento,
        String email,
        String telefone,
        String numeroOab,
        String polo,
        String papel,
        Map<String, String> atributos
) {
    public IdentidadeJuridicaResolucaoEntrada {
        origem = normalizeNullable(origem);
        nome = normalizeNullable(nome);
        documento = normalizeNullable(documento);
        email = normalizeNullable(email);
        telefone = normalizeNullable(telefone);
        numeroOab = normalizeNullable(numeroOab);
        polo = normalizeNullable(polo);
        papel = normalizeNullable(papel);
        atributos = sanitize(atributos);
        if (nome == null && documento == null && email == null && telefone == null && numeroOab == null) {
            throw new IllegalArgumentException("a entrada de resolução exige ao menos um identificador");
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
