package com.tcc.pjb.backend.core.processo.dependencia.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoDependenciaItem(
        Long processoId,
        String numeroProcesso,
        String natureza,
        double score,
        boolean bloquearFluxo,
        List<String> fundamentos
) {
    public ProcessoDependenciaItem {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        natureza = Objects.toString(natureza, "").trim();
        score = Math.max(0d, Math.min(1d, score));
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
