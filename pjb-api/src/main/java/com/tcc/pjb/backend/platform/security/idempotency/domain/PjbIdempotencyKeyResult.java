package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyKeyResult(String key, String status, int retryAfterSeconds) {}
