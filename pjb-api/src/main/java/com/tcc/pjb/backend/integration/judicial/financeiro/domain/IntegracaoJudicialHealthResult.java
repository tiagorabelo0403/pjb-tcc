package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialHealthResult(
        String integracao,
        String status,
        boolean healthy,
        String summary
) {}
