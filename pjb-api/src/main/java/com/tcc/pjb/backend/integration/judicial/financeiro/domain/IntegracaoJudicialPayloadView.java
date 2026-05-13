package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialPayloadView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
