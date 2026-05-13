package com.tcc.pjb.backend.platform.security.idempotency.domain;

import java.time.Instant;

public record PjbIdempotencyHealthSnapshot(boolean enabled, int retryAfterSeconds, Instant capturedAt) {}
