package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloLatencyHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
