package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyExecutionHealthView(
        String key,
        String status,
        boolean healthy,
        String route
) {}
