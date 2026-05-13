package com.tcc.pjb.backend.core.identidade.resolucao.domain;

import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaGraphAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record IdentidadeJuridicaResolucaoAggregate(
        String correlacaoId,
        String solicitante,
        String origemSolicitacao,
        List<IdentidadeJuridicaResolucaoEntrada> entradas,
        List<IdentidadeJuridicaResolucaoItem> itens,
        List<String> conflitos,
        IdentidadeJuridicaGraphAggregate grafo,
        Instant geradoEm
) {
    public IdentidadeJuridicaResolucaoAggregate {
        correlacaoId = Objects.toString(correlacaoId, "").trim();
        solicitante = Objects.toString(solicitante, "").trim();
        origemSolicitacao = Objects.toString(origemSolicitacao, "").trim();
        entradas = entradas == null ? List.of() : List.copyOf(entradas);
        itens = itens == null ? List.of() : List.copyOf(itens);
        conflitos = conflitos == null ? List.of() : List.copyOf(conflitos);
        Objects.requireNonNull(grafo, "grafo");
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
