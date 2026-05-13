package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.common;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaTribunalResponse(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String fallbackNacional,
        String ondaAtual,
        String status,
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
    public PjbSubstituicaoFederativaTribunalResponse {
        tribunalCodigo = Objects.toString(tribunalCodigo, "").trim();
        tribunalNome = Objects.toString(tribunalNome, "").trim();
        ramoJustica = Objects.toString(ramoJustica, "").trim();
        legadoPrincipal = Objects.toString(legadoPrincipal, "").trim();
        fallbackNacional = Objects.toString(fallbackNacional, "").trim();
        ondaAtual = Objects.toString(ondaAtual, "").trim();
        status = Objects.toString(status, "").trim();
        sistemasProntos = sistemasProntos == null ? List.of() : List.copyOf(sistemasProntos);
        sistemasSaudaveis = sistemasSaudaveis == null ? List.of() : List.copyOf(sistemasSaudaveis);
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        rollback = rollback == null ? List.of() : List.copyOf(rollback);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
