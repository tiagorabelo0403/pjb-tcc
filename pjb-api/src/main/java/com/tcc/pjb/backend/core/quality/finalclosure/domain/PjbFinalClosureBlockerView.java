package com.tcc.pjb.backend.core.quality.finalclosure.domain;

public record PjbFinalClosureBlockerView(
        String scope,
        String code,
        String severity,
        String source,
        String summary
) {
}
