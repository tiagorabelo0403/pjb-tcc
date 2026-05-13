package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloAuditView(
        String operation,
        double p50,
        double p95,
        double p99,
        String summary
) {}
