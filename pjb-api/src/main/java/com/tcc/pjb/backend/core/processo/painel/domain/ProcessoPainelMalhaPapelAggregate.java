package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPainelMalhaPapelAggregate(
        Long processoId,
        String numeroProcesso,
        String papel,
        String ramo,
        String statusGeral,
        List<ProcessoPainelContextualWidget> widgets,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPainelMalhaPapelAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        papel = Objects.toString(papel, "CIDADAO").trim();
        ramo = Objects.toString(ramo, "NAO_INFORMADO").trim();
        statusGeral = Objects.toString(statusGeral, "ESTAVEL").trim();
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
