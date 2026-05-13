package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyConsistencyView(
        String key,
        String status,
        boolean consistent,
        String summary
) {}
