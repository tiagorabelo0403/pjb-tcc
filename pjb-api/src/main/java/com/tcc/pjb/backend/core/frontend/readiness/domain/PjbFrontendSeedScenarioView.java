package com.tcc.pjb.backend.core.frontend.readiness.domain;

public record PjbFrontendSeedScenarioView(
        String code,
        String path,
        boolean present,
        int sampleCount,
        boolean mockExternalIntegrations,
        String notes
) {
}
