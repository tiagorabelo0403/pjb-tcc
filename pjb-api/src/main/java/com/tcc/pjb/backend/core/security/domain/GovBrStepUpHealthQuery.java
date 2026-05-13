package com.tcc.pjb.backend.core.security.domain;

public record GovBrStepUpHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
