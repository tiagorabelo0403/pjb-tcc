package com.tcc.pjb.backend.core.processo.producao.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoProducaoPesadaAggregate(
        Long processoId,
        String numeroProcesso,
        List<ProcessoProducaoPesadaGate> gates,
        int scoreGeral,
        PjbFechamentoStatus statusGeral,
        boolean prontoProducaoPesada,
        List<String> bloqueios,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoProducaoPesadaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        gates = gates == null ? List.of() : List.copyOf(gates);
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        statusGeral = statusGeral == null ? PjbFechamentoStatus.PENDENTE : statusGeral;
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
