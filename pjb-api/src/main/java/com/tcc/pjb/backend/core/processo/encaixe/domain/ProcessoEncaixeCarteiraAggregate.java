package com.tcc.pjb.backend.core.processo.encaixe.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoEncaixeCarteiraAggregate(
        int totalEscaneados,
        long totalBloqueantes,
        long scoreMedio,
        List<ProcessoEncaixeResumo> processos,
        List<ProcessoEncaixeFinding> tendencias,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoEncaixeCarteiraAggregate {
        processos = processos == null ? List.of() : List.copyOf(processos);
        tendencias = tendencias == null ? List.of() : List.copyOf(tendencias);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
