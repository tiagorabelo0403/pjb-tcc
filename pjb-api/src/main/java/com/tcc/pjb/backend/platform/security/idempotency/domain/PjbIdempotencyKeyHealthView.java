package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyKeyHealthView(String key, boolean present, String status) {}
