package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialRetryAuditView(
        String reference,
        String status,
        String summary
) {
}
