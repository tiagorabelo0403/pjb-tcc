package com.tcc.pjb.backend.core.quality.finalclosure.domain;

import java.time.Instant;
import java.util.List;

public record PjbFinalClosureSummary(
        boolean overallReady,
        boolean buildApproved,
        boolean codebaseClean,
        boolean apiSurfaceClean,
        boolean modularizationReady,
        boolean roadmapClosed,
        boolean endToEndValidated,
        int totalMacroblocks,
        int closedMacroblocks,
        int partialMacroblocks,
        int notStartedMacroblocks,
        int blockerCount,
        int adminControllers,
        int applicationServices,
        int surfacedMacroblocks,
        List<String> criticalBlockers,
        Instant generatedAt
) {
}
