package com.tcc.pjb.backend.core.security.domain;

public record GovBrStepUpAuditView(
        String reference,
        String status,
        String summary
) {
}
