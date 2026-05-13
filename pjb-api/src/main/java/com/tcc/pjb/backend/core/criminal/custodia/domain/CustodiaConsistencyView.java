package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record CustodiaConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
