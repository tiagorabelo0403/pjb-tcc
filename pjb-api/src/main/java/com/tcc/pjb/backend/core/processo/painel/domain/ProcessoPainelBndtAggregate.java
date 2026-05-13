package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelBndtAggregate(
        Long processoId,
        String numeroProcesso,
        boolean aplicavel,
        String status,
        boolean consultaTempoReal,
        String fonteOficial,
        String fallbackMode,
        List<String> alertas,
        List<String> proximosPassos,
        Instant geradoEm
) {
    public ProcessoPainelBndtAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        status = status == null ? "NAO_APLICAVEL" : status;
        fonteOficial = fonteOficial == null ? "BNDT" : fonteOficial;
        fallbackMode = fallbackMode == null ? "REPLAY_CONTROLADO" : fallbackMode;
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        proximosPassos = proximosPassos == null ? List.of() : List.copyOf(proximosPassos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
