package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialWindowHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
