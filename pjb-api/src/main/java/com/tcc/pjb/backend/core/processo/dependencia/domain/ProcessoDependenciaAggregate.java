package com.tcc.pjb.backend.core.processo.dependencia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoDependenciaAggregate(
        Long processoIdRaiz,
        String numeroProcessoRaiz,
        boolean haDependencia,
        List<ProcessoDependenciaItem> itens,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoDependenciaAggregate {
        numeroProcessoRaiz = Objects.toString(numeroProcessoRaiz, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
