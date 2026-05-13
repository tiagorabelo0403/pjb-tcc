package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.time.Instant;

public record PjbBackendReadyForFrontendSummary(
        boolean readyForFrontend,
        boolean buildGateApproved,
        boolean apiSurfaceClean,
        boolean authContractReady,
        boolean errorContractReady,
        boolean publicRouteCatalogReady,
        boolean controllerCoverageReady,
        boolean frontendBootstrapReady,
        int publicRouteCount,
        int adminRouteCount,
        int uiRouteCount,
        int blockerCount,
        Instant generatedAt
) {
}
