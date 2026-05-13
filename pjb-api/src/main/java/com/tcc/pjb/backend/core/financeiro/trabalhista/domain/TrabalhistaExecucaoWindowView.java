package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

public record TrabalhistaExecucaoWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
