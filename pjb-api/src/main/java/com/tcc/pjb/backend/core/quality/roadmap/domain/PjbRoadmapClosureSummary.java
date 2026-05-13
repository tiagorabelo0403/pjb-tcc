package com.tcc.pjb.backend.core.quality.roadmap.domain;

import java.time.Instant;

public record PjbRoadmapClosureSummary(
        int total,
        int closed,
        int partial,
        int notStarted,
        int completionPercent,
        int surfacedPartial,
        int blockingCount,
        Instant generatedAt
) {
    public PjbRoadmapClosureSummary {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
