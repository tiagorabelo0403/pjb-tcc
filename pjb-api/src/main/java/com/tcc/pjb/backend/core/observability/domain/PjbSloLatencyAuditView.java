package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloLatencyAuditView(
        String reference,
        String status,
        String summary
) {
}
