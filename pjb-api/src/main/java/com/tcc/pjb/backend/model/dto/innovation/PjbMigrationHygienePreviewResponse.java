package com.tcc.pjb.backend.model.dto.innovation;

import java.util.List;

public record PjbMigrationHygienePreviewResponse(
        String sourceSystem,
        String readiness,
        List<String> blockers,
        List<String> sanitationActions,
        List<String> automationOpportunities,
        String suggestedJourney,
        int readinessScore
) {
}
