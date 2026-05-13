package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialConsistencyView(
        String integracao,
        boolean consistent,
        String summary,
        String source
) {}
