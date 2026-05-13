package com.tcc.pjb.backend.integration.mni.domain;

public record MniTribunalWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
