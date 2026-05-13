package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialPayloadWindowView(
        String reference,
        String status,
        String summary
) {
}
