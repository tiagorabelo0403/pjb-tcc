package com.tcc.pjb.backend.core.processo.analytics.domain;

public record ProcessoAnalyticsUnidade(
        String unidade,
        long totalProcessos,
        double tempoMedioDias,
        double taxaUrgencia,
        double riscoSla,
        String faixa
) {
    public ProcessoAnalyticsUnidade {
        unidade = unidade == null || unidade.isBlank() ? "SEM_UNIDADE" : unidade;
        totalProcessos = Math.max(0L, totalProcessos);
        faixa = faixa == null || faixa.isBlank() ? "OBSERVAR" : faixa;
    }
}
