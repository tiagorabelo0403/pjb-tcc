package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyReplayView(String key, boolean replayed, String status) {}
