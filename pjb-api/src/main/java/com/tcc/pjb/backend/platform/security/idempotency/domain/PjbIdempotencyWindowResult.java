package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyWindowResult(String key, int retryAfterSeconds, boolean active) {}
