package com.tcc.pjb.backend.core.prazos.policy.domain;

public record PrazoPolicyHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
