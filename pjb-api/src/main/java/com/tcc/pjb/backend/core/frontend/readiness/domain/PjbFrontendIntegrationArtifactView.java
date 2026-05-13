package com.tcc.pjb.backend.core.frontend.readiness.domain;

public record PjbFrontendIntegrationArtifactView(
        String category,
        String name,
        String path,
        boolean present,
        String notes
) {
}
