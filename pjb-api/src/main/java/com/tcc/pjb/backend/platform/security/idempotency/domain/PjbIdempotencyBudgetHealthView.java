package com.tcc.pjb.backend.platform.security.idempotency.domain;

import java.time.Duration;

public record PjbIdempotencyBudgetHealthView(
        Duration ttl,
        int retryAfterSeconds,
        boolean healthy,
        String summary
) {}
