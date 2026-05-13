package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.time.Instant;

public record PjbFrontendIntegrationPackSummary(
        boolean ready,
        boolean openApiExported,
        boolean postmanExported,
        boolean seedPackPresent,
        boolean errorCatalogPresent,
        boolean frontendDevProfilePresent,
        boolean smokePackPresent,
        int artifactCount,
        Instant generatedAt
) {
}
