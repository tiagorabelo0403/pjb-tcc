package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialTimelineAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
