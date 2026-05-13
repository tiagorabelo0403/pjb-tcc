package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyBudgetView(
        String reference,
        String status,
        String summary
) {
}
