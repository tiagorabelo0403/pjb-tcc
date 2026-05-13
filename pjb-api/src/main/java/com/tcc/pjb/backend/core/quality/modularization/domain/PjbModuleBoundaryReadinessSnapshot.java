package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.time.Instant;
import java.util.List;

public record PjbModuleBoundaryReadinessSnapshot(
        boolean aggregatorPomPresent,
        boolean coreExtractionReady,
        int estimatedBoundaryViolations,
        int candidateCorePackages,
        int controllerPackages,
        int servicePackages,
        List<PjbModuleBoundaryIssue> blockers,
        List<String> recommendedNextActions,
        Instant generatedAt
) {
    public PjbModuleBoundaryReadinessSnapshot {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        recommendedNextActions = recommendedNextActions == null ? List.of() : List.copyOf(recommendedNextActions);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
