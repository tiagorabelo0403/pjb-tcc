package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyReplayHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
