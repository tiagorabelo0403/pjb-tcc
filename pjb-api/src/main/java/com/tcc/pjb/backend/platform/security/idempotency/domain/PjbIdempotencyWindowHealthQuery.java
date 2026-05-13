package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyWindowHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
