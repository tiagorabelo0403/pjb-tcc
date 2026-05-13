package com.tcc.pjb.backend.core.frontend.readiness.domain;

public record PjbBackendReadinessChecklistItem(
        String code,
        String title,
        String status,
        String severity,
        String detail,
        String nextAction
) {
}
