package com.tcc.pjb.backend.core.processo.analytics.domain;

import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessoAnalyticsNacionalAggregate(
        Long processoId,
        Map<String, String> recorte,
        ProcessoAnalyticsAggregate baseline,
        double taxaCongestionamento,
        double taxaRetrabalho,
        long mapaUrgencia,
        double riscoSlaGlobal,
        List<ProcessoAnalyticsUnidade> unidadesCriticas,
        List<ProcessoAnalyticsFila> gargalosFila,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoAnalyticsNacionalAggregate {
        recorte = recorte == null ? Map.of() : Map.copyOf(recorte);
        unidadesCriticas = unidadesCriticas == null ? List.of() : List.copyOf(unidadesCriticas);
        gargalosFila = gargalosFila == null ? List.of() : List.copyOf(gargalosFila);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
