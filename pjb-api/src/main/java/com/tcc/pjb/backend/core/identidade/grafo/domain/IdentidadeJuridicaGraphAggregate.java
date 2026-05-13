package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record IdentidadeJuridicaGraphAggregate(
        String correlacaoId,
        String solicitante,
        String origemSolicitacao,
        List<IdentidadeJuridicaVertice> vertices,
        List<IdentidadeJuridicaAresta> arestas,
        List<IdentidadeJuridicaCaminho> conexoesOcultas,
        List<IdentidadeJuridicaAchado> achados,
        IdentidadeJuridicaResumo resumo,
        IdentidadeJuridicaPersistencia persistencia,
        List<String> fundamentos,
        Instant geradoEm,
        Duration tempoProcessamento
) {
    public IdentidadeJuridicaGraphAggregate {
        vertices = vertices == null ? List.of() : List.copyOf(vertices);
        arestas = arestas == null ? List.of() : List.copyOf(arestas);
        conexoesOcultas = conexoesOcultas == null ? List.of() : List.copyOf(conexoesOcultas);
        achados = achados == null ? List.of() : List.copyOf(achados);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
        tempoProcessamento = tempoProcessamento == null || tempoProcessamento.isNegative() ? Duration.ZERO : tempoProcessamento;
    }
}
