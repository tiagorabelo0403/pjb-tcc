package com.tcc.pjb.backend.platform.security.idempotency.domain;

import java.time.Instant;

public record PjbIdempotencyKeySnapshot(String key, String status, Instant capturedAt) {}
