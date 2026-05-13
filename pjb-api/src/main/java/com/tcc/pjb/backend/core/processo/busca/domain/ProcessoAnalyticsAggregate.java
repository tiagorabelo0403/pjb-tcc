package com.tcc.pjb.backend.core.processo.busca.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessoAnalyticsAggregate(
        Map<String, String> escopo,
        long totalProcessos,
        long totalAtivos,
        double tempoMedioDias,
        double taxaRecursal,
        double taxaAcordo,
        double taxaUrgencia,
        List<ProcessoAnalyticsIndicador> indicadores,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoAnalyticsAggregate {
        escopo = escopo == null ? Map.of() : Map.copyOf(escopo);
        indicadores = indicadores == null ? List.of() : List.copyOf(indicadores);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
