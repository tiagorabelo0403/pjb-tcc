package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.time.Instant;
import java.util.List;

public record PjbCoreExtractionSnapshot(
        boolean readyForScaffold,
        boolean aggregatorPomPresent,
        int candidatePackageCount,
        int safeCandidateCount,
        int dependencyIssueCount,
        List<PjbCoreExtractionDependencyIssue> issues,
        List<String> recommendedSteps,
        Instant generatedAt
) {
    public PjbCoreExtractionSnapshot {
        issues = issues == null ? List.of() : List.copyOf(issues);
        recommendedSteps = recommendedSteps == null ? List.of() : List.copyOf(recommendedSteps);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
