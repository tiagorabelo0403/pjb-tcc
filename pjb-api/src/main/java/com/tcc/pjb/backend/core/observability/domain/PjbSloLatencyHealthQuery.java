package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloLatencyHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
