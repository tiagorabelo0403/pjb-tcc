package com.tcc.pjb.backend.core.quality.gates.domain;

import java.util.List;

public record PjbMutationQualityView(
        boolean pluginPresent,
        boolean thresholdConfigured,
        boolean qualityWorkflowPresent,
        boolean targetClassesConfigured,
        boolean ready,
        List<String> details
) {
    public PjbMutationQualityView {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
