package com.tcc.pjb.backend.core.security.domain;

public record GovBrPolicyHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
