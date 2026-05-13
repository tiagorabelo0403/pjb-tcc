package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record IdentidadeJuridicaConsulta(
        String correlacaoId,
        String solicitante,
        List<IdentidadeJuridicaSemente> sementes,
        List<String> processosRaiz,
        int profundidadeMaxima,
        int limiteVertices,
        int limiteArestas,
        boolean persistirNoGrafo,
        boolean publicarEvento,
        String origemSolicitacao
) {
    public IdentidadeJuridicaConsulta {
        correlacaoId = normalizeNullable(correlacaoId);
        solicitante = normalizeNullable(solicitante);
        sementes = sementes == null ? List.of() : List.copyOf(sementes);
        processosRaiz = sanitize(processosRaiz);
        profundidadeMaxima = Math.max(1, profundidadeMaxima == 0 ? 4 : profundidadeMaxima);
        limiteVertices = Math.max(20, limiteVertices == 0 ? 600 : limiteVertices);
        limiteArestas = Math.max(20, limiteArestas == 0 ? 1600 : limiteArestas);
        origemSolicitacao = normalizeNullable(origemSolicitacao);
        if (sementes.isEmpty() && processosRaiz.isEmpty()) {
            throw new IllegalArgumentException("a consulta exige sementes ou processos raiz");
        }
    }

    private static List<String> sanitize(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : source) {
            String normalized = normalizeNullable(item);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return List.copyOf(values);
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
