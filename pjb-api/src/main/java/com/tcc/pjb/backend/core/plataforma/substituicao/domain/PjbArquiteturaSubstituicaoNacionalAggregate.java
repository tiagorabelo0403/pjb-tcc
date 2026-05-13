package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;

public record PjbArquiteturaSubstituicaoNacionalAggregate(
        int scoreGeral,
        boolean prontoParaSubstituicaoImediata,
        boolean buildGateAprovado,
        long totalProcessos,
        long totalWorkItemsPendentes,
        long totalWorkItemsExpirados,
        long totalTribunaisCatalogados,
        long totalRitosCatalogados,
        List<PjbArquiteturaSubstituicaoPilar> pilares,
        String conclusaoTecnica,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbArquiteturaSubstituicaoNacionalAggregate {
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        totalProcessos = Math.max(0L, totalProcessos);
        totalWorkItemsPendentes = Math.max(0L, totalWorkItemsPendentes);
        totalWorkItemsExpirados = Math.max(0L, totalWorkItemsExpirados);
        totalTribunaisCatalogados = Math.max(0L, totalTribunaisCatalogados);
        totalRitosCatalogados = Math.max(0L, totalRitosCatalogados);
        pilares = pilares == null ? List.of() : List.copyOf(pilares);
        conclusaoTecnica = conclusaoTecnica == null ? "" : conclusaoTecnica.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
