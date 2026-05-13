package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoDecisionHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
