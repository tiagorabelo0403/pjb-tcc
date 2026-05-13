package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPainelMalhaNacionalAggregate(
        Long processoId,
        String numeroProcesso,
        String statusGeral,
        long totalRiscos,
        long totalBloqueios,
        List<ProcessoPainelContextualWidget> widgets,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPainelMalhaNacionalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        statusGeral = Objects.toString(statusGeral, "ESTAVEL").trim();
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
