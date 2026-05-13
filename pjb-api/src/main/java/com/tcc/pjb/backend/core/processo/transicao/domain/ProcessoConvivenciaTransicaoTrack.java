package com.tcc.pjb.backend.core.processo.transicao.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.util.List;
import java.util.Objects;

public record ProcessoConvivenciaTransicaoTrack(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        int score,
        boolean reversivel,
        String modoExecucao,
        String criterioEquivalencia,
        List<String> fundamentos
) {
    public ProcessoConvivenciaTransicaoTrack {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        modoExecucao = Objects.toString(modoExecucao, "").trim();
        criterioEquivalencia = Objects.toString(criterioEquivalencia, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
