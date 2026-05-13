package com.tcc.pjb.backend.model.dto.ai.legal.eval;

import java.util.Map;

public record LegalEvalMetric(
        String metricId,
        String label,
        double score,
        double threshold,
        boolean passed,
        Object observed,
        Object expected
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "metricId", metricId,
                "label", label,
                "score", score,
                "threshold", threshold,
                "passed", passed,
                "observed", observed == null ? "" : observed,
                "expected", expected == null ? "" : expected
        );
    }
}
