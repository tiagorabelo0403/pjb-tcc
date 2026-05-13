package com.tcc.pjb.backend.core.quality.gates.domain;

import java.util.List;

public record PjbContractQualityView(
        boolean pactDependencyPresent,
        boolean contractTestsPresent,
        boolean pactOutputConfigured,
        boolean qualityWorkflowPresent,
        int matchingTests,
        boolean ready,
        List<String> details
) {
    public PjbContractQualityView {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
