package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPainelRiscoMalhaAggregate(
        Long processoId,
        String numeroProcesso,
        String statusGeral,
        int scoreGlobal,
        boolean possuiBloqueio,
        List<ProcessoPainelContextualWidget> widgets,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPainelRiscoMalhaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        statusGeral = Objects.toString(statusGeral, "ESTAVEL").trim();
        scoreGlobal = Math.max(0, scoreGlobal);
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
