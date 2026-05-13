package com.tcc.pjb.backend.core.security.domain;

public record GovBrSensitiveActHealthView(
        String reference,
        String status,
        String summary
) {
}
