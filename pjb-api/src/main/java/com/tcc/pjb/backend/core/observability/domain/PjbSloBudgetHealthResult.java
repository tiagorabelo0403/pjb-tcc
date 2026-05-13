package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloBudgetHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
