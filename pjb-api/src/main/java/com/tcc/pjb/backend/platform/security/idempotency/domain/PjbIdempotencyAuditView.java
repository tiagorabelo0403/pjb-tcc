package com.tcc.pjb.backend.platform.security.idempotency.domain;

import java.time.Instant;

public record PjbIdempotencyAuditView(String key, String status, Instant at) {}
