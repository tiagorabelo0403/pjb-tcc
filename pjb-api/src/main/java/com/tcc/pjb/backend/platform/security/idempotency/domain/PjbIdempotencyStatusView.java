package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyStatusView(String key, String status) {}
