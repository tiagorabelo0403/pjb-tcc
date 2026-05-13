package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloOperationWindowQuery(
        String reference,
        String scope,
        Integer limit
) {
}
