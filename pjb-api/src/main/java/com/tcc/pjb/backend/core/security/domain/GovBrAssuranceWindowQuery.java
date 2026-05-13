package com.tcc.pjb.backend.core.security.domain;

public record GovBrAssuranceWindowQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
