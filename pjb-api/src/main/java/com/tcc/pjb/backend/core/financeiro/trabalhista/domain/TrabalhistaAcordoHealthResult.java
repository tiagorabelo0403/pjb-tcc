package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

public record TrabalhistaAcordoHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
