package com.tcc.pjb.backend.core.frontend.delivery.domain;

import java.time.Instant;

public record PjbFrontendDeliverySummary(
        boolean readyForFrontend,
        boolean buildGateApproved,
        boolean apiSurfaceClean,
        boolean roadmapClosureReady,
        int totalRoutes,
        int adminRoutes,
        int uiRoutes,
        int publicRoutes,
        int controllerCount,
        int surfacedMacroblocks,
        int blockerCount,
        Instant generatedAt
) {
}
