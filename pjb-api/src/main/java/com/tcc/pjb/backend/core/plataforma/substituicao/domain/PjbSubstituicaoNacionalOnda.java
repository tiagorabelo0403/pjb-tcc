package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoNacionalOnda(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        int score,
        boolean pronta,
        String objetivo,
        List<String> criteriosEntrada,
        List<String> blocosExecucao,
        List<String> guardrails,
        List<String> rollback,
        List<String> sistemasAlvo,
        List<String> proximasAcoes
) {
    public PjbSubstituicaoNacionalOnda {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        objetivo = Objects.toString(objetivo, "").trim();
        criteriosEntrada = criteriosEntrada == null ? List.of() : List.copyOf(criteriosEntrada);
        blocosExecucao = blocosExecucao == null ? List.of() : List.copyOf(blocosExecucao);
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        rollback = rollback == null ? List.of() : List.copyOf(rollback);
        sistemasAlvo = sistemasAlvo == null ? List.of() : List.copyOf(sistemasAlvo);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
