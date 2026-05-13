package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoLegadosSistema(
        String sistema,
        PjbFechamentoStatus status,
        int scoreAderencia,
        String conclusao,
        List<String> pendencias
) {
    public PjbSubstituicaoLegadosSistema {
        sistema = Objects.toString(sistema, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        scoreAderencia = Math.max(0, Math.min(100, scoreAderencia));
        conclusao = Objects.toString(conclusao, "").trim();
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
    }
}
