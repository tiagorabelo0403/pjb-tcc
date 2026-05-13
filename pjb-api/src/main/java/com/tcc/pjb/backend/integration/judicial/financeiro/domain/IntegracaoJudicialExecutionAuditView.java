package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialExecutionAuditView(
        String reference,
        String status,
        String summary
) {
}
