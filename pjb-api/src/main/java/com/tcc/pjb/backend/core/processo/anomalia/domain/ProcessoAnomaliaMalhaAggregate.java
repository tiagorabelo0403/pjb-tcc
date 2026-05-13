package com.tcc.pjb.backend.core.processo.anomalia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoAnomaliaMalhaAggregate(
        Long processoId,
        String numeroProcesso,
        String nivelGlobal,
        int scoreGlobal,
        boolean exigeEscalonamento,
        List<ProcessoAnomaliaMalhaItem> itens,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoAnomaliaMalhaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        nivelGlobal = Objects.toString(nivelGlobal, "").trim();
        scoreGlobal = Math.max(0, Math.min(100, scoreGlobal));
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
