package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyWindowHealthView(
        String key,
        long remainingSeconds,
        boolean healthy,
        String summary
) {}
