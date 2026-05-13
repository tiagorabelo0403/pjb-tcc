package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloOperationWindowResult(
        boolean available,
        String summary,
        Long total
) {
}
