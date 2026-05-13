package com.tcc.pjb.backend.core.processo.timeline.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTimelineAggregate(
        ProcessoTimelineIdentity identity,
        long totalEventos,
        long totalPendencias,
        long totalBloqueantes,
        List<String> eixosAtivos,
        List<ProcessoTimelineEvento> eventos,
        List<ProcessoTimelinePendencia> pendencias,
        List<String> proximoCiclo,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoTimelineAggregate {
        Objects.requireNonNull(identity);
        eixosAtivos = eixosAtivos == null ? List.of() : List.copyOf(eixosAtivos);
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
        proximoCiclo = proximoCiclo == null ? List.of() : List.copyOf(proximoCiclo);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
