package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyDecisionView(String key, boolean acquired, String status) {}
