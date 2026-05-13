package com.tcc.pjb.backend.core.quality.gates.domain;

public record PjbQualityBlockerView(
        String scope,
        String code,
        String severity,
        String summary
) {
}
