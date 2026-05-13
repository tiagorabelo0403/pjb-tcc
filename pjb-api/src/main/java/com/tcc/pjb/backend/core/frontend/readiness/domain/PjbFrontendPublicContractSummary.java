package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.time.Instant;

public record PjbFrontendPublicContractSummary(
        boolean ready,
        boolean authContractReady,
        boolean errorContractReady,
        boolean openApiAvailable,
        int publicRouteCount,
        int anonymousRouteCount,
        int authenticatedRouteCount,
        int dtoCatalogCount,
        Instant generatedAt
) {
}
