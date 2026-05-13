package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoLegadosProva(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        int score,
        boolean concluida,
        List<String> fundamentos,
        List<String> bloqueios
) {
    public PjbSubstituicaoLegadosProva {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
    }
}
