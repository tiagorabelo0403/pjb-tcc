package com.tcc.pjb.backend.core.processo.migracao.intelligence;

import java.util.List;

public record PjbLegacyMigrationIntelligenceReport(
        String status,
        boolean readyForExecution,
        List<PjbLegacyMigrationDivergence> divergences,
        List<String> requiredActions
) {
    public PjbLegacyMigrationIntelligenceReport {
        status = status == null || status.isBlank() ? "REVIEW_REQUIRED" : status.trim();
        divergences = divergences == null ? List.of() : List.copyOf(divergences);
        requiredActions = requiredActions == null ? List.of() : List.copyOf(requiredActions);
    }
}
