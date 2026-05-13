package com.tcc.pjb.backend.core.security.domain;

public record GovBrPolicyHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
