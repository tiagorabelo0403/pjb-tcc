package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyReplayAuditView(
        String key,
        boolean replayed,
        String status,
        String summary
) {}
