package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.util.List;
import java.util.Objects;

public record IdentidadeJuridicaSnapshot(
        String fonteCodigo,
        boolean degradada,
        List<IdentidadeJuridicaVertice> vertices,
        List<IdentidadeJuridicaAresta> arestas,
        List<String> fundamentos,
        String diagnostico
) {
    public IdentidadeJuridicaSnapshot {
        fonteCodigo = Objects.toString(fonteCodigo, "DESCONHECIDA").trim();
        vertices = vertices == null ? List.of() : List.copyOf(vertices);
        arestas = arestas == null ? List.of() : List.copyOf(arestas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        diagnostico = normalizeNullable(diagnostico);
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
