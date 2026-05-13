package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaWarRoomTribunal(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String ondaAtual,
        String status,
        int scoreProntidao,
        boolean janelaAberta,
        boolean freezeAtivo,
        boolean corteLiberado,
        String janelaAtual,
        List<PjbSubstituicaoFederativaWarRoomRamo> ramos,
        List<String> guardrails,
        List<String> rollback,
        List<String> bloqueadores,
        List<String> proximasAcoes
) {
    public PjbSubstituicaoFederativaWarRoomTribunal {
        tribunalCodigo = Objects.toString(tribunalCodigo, "").trim();
        tribunalNome = Objects.toString(tribunalNome, "").trim();
        ramoJustica = Objects.toString(ramoJustica, "").trim();
        ondaAtual = Objects.toString(ondaAtual, "").trim();
        status = Objects.toString(status, "").trim();
        scoreProntidao = Math.max(0, Math.min(100, scoreProntidao));
        janelaAtual = Objects.toString(janelaAtual, "").trim();
        ramos = ramos == null ? List.of() : List.copyOf(ramos);
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        rollback = rollback == null ? List.of() : List.copyOf(rollback);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
