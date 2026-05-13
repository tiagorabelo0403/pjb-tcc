package com.tcc.pjb.backend.core.security.domain;

public record GovBrAssuranceConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
