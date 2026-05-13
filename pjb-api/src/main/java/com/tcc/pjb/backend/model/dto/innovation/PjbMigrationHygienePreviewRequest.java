package com.tcc.pjb.backend.model.dto.innovation;

public record PjbMigrationHygienePreviewRequest(
        String sourceSystem,
        boolean pendingSignatures,
        boolean hearingScheduled,
        boolean judgmentScheduled,
        boolean openDeadlines,
        boolean pendingTribunalAppeals,
        boolean missingNationalIds,
        boolean tpuClassificationConsistent,
        boolean suspended,
        boolean archived,
        int mediaCount,
        boolean collegiateCase
) {
}
