package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbArquiteturaSubstituicaoPilar(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        int score,
        boolean pronto,
        List<PjbArquiteturaSubstituicaoCapacidade> capacidades,
        List<String> proximasAcoes
) {
    public PjbArquiteturaSubstituicaoPilar {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        capacidades = capacidades == null ? List.of() : List.copyOf(capacidades);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
