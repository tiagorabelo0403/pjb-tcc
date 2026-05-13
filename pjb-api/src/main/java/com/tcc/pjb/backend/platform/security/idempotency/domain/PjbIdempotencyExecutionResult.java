package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyExecutionResult(String key, boolean acquired, String status) {}
