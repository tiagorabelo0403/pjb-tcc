package com.tcc.pjb.backend.core.security.domain;

public record GovBrStepUpHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
