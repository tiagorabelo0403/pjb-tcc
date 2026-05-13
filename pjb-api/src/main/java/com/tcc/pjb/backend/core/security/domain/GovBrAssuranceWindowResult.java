package com.tcc.pjb.backend.core.security.domain;

public record GovBrAssuranceWindowResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
