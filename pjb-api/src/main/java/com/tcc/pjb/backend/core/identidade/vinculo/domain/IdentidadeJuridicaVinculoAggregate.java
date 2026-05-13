package com.tcc.pjb.backend.core.identidade.vinculo.domain;

import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaGraphAggregate;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record IdentidadeJuridicaVinculoAggregate(
        Long processoId,
        String numeroProcesso,
        List<IdentidadeJuridicaVinculoParte> partes,
        List<IdentidadeJuridicaVinculoItem> itens,
        List<String> alertas,
        IdentidadeJuridicaResolucaoAggregate resolucao,
        IdentidadeJuridicaGraphAggregate grafo,
        Instant geradoEm
) {
    public IdentidadeJuridicaVinculoAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        partes = partes == null ? List.of() : List.copyOf(partes);
        itens = itens == null ? List.of() : List.copyOf(itens);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        Objects.requireNonNull(resolucao, "resolucao");
        Objects.requireNonNull(grafo, "grafo");
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
