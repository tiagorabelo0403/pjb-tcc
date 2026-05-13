package com.tcc.pjb.backend.core.quality.gates.domain;

import java.util.List;

public record PjbDastQualityView(
        boolean workflowPresent,
        boolean rulesPresent,
        boolean openApiScanConfigured,
        boolean stagingGuardPresent,
        boolean ready,
        List<String> details
) {
    public PjbDastQualityView {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
