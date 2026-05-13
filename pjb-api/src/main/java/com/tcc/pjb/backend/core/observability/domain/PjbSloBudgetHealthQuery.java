package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloBudgetHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
