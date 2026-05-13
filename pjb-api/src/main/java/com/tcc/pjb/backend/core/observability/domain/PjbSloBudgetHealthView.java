package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloBudgetHealthView(
        String operation,
        double sloSeconds,
        boolean healthy,
        String summary
) {}
