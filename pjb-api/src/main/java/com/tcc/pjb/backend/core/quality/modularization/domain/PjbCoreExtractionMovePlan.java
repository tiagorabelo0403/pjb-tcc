package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbCoreExtractionMovePlan(
        String phase,
        String scope,
        int estimatedFiles,
        List<String> candidatePackages,
        List<String> blockers,
        List<String> steps
) {
    public PjbCoreExtractionMovePlan {
        phase = phase == null ? "" : phase;
        scope = scope == null ? "" : scope;
        candidatePackages = candidatePackages == null ? List.of() : List.copyOf(candidatePackages);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
