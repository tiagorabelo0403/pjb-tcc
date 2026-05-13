package com.tcc.pjb.backend.core.processo.timeline.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTimelineMalhaAggregate(
        Long processoId,
        String numeroProcesso,
        int totalEventosMalha,
        int totalBloqueiosMalha,
        String proximaAcaoOperacional,
        List<String> hotspots,
        List<ProcessoTimelineMalhaEvento> eventos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoTimelineMalhaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        totalEventosMalha = Math.max(0, totalEventosMalha);
        totalBloqueiosMalha = Math.max(0, totalBloqueiosMalha);
        proximaAcaoOperacional = Objects.toString(proximaAcaoOperacional, "").trim();
        hotspots = hotspots == null ? List.of() : List.copyOf(hotspots);
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
