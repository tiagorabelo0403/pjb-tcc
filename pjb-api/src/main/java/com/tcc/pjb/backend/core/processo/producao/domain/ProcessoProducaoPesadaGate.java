package com.tcc.pjb.backend.core.processo.producao.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.util.List;
import java.util.Objects;

public record ProcessoProducaoPesadaGate(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        int score,
        boolean bloqueante,
        String diagnostico,
        String proximaAcao,
        List<String> evidencias
) {
    public ProcessoProducaoPesadaGate {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        diagnostico = Objects.toString(diagnostico, "").trim();
        proximaAcao = Objects.toString(proximaAcao, "").trim();
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
    }
}
