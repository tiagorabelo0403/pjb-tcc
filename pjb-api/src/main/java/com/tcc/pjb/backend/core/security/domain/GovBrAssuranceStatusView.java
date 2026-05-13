package com.tcc.pjb.backend.core.security.domain;

public record GovBrAssuranceStatusView(
        String reference,
        String status,
        String summary
) {
}
