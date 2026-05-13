package com.tcc.pjb.backend.core.kernel.recursal.plan;

import java.util.List;

public record GraphSnapshot(
        Long caseFileId,
        String anchorProceedingKey,
        List<ProceedingView> proceedings,
        List<EdgeView> edges
) {

    public GraphSnapshot {
        proceedings = proceedings == null ? List.of() : List.copyOf(proceedings);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }
}
