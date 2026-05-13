package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialStatusResult(
        boolean available,
        String summary,
        Long total
) {
}
