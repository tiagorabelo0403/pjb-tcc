package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialWindowHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
