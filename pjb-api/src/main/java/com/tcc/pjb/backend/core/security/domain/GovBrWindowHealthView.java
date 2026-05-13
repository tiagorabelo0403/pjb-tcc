package com.tcc.pjb.backend.core.security.domain;

public record GovBrWindowHealthView(
        String reference,
        String status,
        String summary
) {
}
