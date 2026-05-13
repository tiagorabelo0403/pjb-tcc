package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyStatusHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
