package com.tcc.pjb.backend.core.quality.roadmap.domain;

import java.util.List;

public record PjbRoadmapQualityGateView(
        boolean buildApproved,
        boolean apiSurfaceClean,
        boolean codebaseClean,
        boolean modularizationReady,
        int totalOutstandingIssues,
        List<String> recommendations,
        List<String> criticalModules
) {
    public PjbRoadmapQualityGateView {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        criticalModules = criticalModules == null ? List.of() : List.copyOf(criticalModules);
    }
}
