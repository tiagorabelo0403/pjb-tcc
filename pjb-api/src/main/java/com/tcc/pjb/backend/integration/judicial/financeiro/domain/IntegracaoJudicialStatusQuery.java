package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialStatusQuery(
        String reference,
        String scope,
        Integer limit
) {
}
