package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloBudgetAuditView(
        String reference,
        String status,
        String summary
) {
}
