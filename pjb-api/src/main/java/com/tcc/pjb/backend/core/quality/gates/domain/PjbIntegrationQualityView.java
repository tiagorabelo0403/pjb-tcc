package com.tcc.pjb.backend.core.quality.gates.domain;

import java.util.List;

public record PjbIntegrationQualityView(
        boolean baseClassPresent,
        boolean profilePresent,
        boolean flywayEnabledInProfile,
        int integrationTests,
        boolean ready,
        List<String> details
) {
    public PjbIntegrationQualityView {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
