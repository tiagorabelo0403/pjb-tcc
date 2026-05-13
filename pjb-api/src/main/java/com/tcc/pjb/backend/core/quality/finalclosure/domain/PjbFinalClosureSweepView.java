package com.tcc.pjb.backend.core.quality.finalclosure.domain;

import java.time.Instant;
import java.util.List;

public record PjbFinalClosureSweepView(
        int adminControllers,
        int applicationServices,
        int surfacedMacroblocks,
        int partialMacroblocksWithoutAdminSurface,
        List<String> highlightedControllers,
        List<String> highlightedApplicationServices,
        Instant generatedAt
) {
}
