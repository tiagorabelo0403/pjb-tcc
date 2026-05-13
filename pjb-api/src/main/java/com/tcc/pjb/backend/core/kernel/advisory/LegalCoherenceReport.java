package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;

public record LegalCoherenceReport(
        double score,
        boolean blocking,
        List<Issue> issues,
        List<String> strengths,
        List<String> strategicRecommendations
) {
    public record Issue(
            String code,
            String title,
            String description,
            String severity,
            boolean blocking,
            List<String> evidence
    ) {
    }
}
