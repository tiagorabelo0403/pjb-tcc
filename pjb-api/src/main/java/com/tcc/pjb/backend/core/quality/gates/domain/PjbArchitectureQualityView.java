package com.tcc.pjb.backend.core.quality.gates.domain;

import java.util.List;

public record PjbArchitectureQualityView(
        boolean archUnitDependencyPresent,
        boolean architectureTestPresent,
        boolean governanceScannerCoveragePresent,
        int matchingTests,
        boolean ready,
        List<String> details
) {
    public PjbArchitectureQualityView {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
