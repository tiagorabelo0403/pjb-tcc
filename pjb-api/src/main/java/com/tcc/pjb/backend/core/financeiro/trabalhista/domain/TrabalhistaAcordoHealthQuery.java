package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

public record TrabalhistaAcordoHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
