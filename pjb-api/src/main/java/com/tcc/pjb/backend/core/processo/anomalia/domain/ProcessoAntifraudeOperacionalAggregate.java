package com.tcc.pjb.backend.core.processo.anomalia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoAntifraudeOperacionalAggregate(
        Long processoId,
        String numeroProcesso,
        String nivelGlobal,
        int scoreGlobal,
        List<String> destinatarios,
        List<ProcessoAnomaliaMalhaItem> itensAcionados,
        List<String> acoesExecutadas,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoAntifraudeOperacionalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        nivelGlobal = Objects.toString(nivelGlobal, "NORMAL").trim();
        scoreGlobal = Math.max(0, Math.min(100, scoreGlobal));
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        itensAcionados = itensAcionados == null ? List.of() : List.copyOf(itensAcionados);
        acoesExecutadas = acoesExecutadas == null ? List.of() : List.copyOf(acoesExecutadas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
