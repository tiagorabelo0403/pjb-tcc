package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

public record TrabalhistaDepositoConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
