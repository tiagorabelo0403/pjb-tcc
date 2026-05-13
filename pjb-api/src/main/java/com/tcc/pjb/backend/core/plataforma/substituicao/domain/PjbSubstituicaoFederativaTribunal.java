package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaTribunal(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String fallbackNacional,
        String ondaAtual,
        PjbFechamentoStatus status,
        int scoreProntidao,
        boolean prontoRollout,
        boolean prontoRollback,
        List<String> sistemasProntos,
        List<String> sistemasSaudaveis,
        List<String> guardrails,
        List<String> rollback,
        List<String> bloqueadores,
        List<String> proximasAcoes
) {
    public PjbSubstituicaoFederativaTribunal {
        tribunalCodigo = Objects.toString(tribunalCodigo, "").trim();
        tribunalNome = Objects.toString(tribunalNome, "").trim();
        ramoJustica = Objects.toString(ramoJustica, "").trim();
        legadoPrincipal = Objects.toString(legadoPrincipal, "").trim();
        fallbackNacional = Objects.toString(fallbackNacional, "").trim();
        ondaAtual = Objects.toString(ondaAtual, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        sistemasProntos = sistemasProntos == null ? List.of() : List.copyOf(sistemasProntos);
        sistemasSaudaveis = sistemasSaudaveis == null ? List.of() : List.copyOf(sistemasSaudaveis);
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        rollback = rollback == null ? List.of() : List.copyOf(rollback);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
