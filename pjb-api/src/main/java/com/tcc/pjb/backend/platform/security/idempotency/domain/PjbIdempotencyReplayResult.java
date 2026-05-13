package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyReplayResult(String key, boolean replayable, String status) {}
