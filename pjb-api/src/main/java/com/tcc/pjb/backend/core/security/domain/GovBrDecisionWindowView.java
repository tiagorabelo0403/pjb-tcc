package com.tcc.pjb.backend.core.security.domain;

public record GovBrDecisionWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
