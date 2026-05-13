package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record ExplainableDecisionTrailReport(
        String scope,
        String status,
        double confidence,
        List<DecisionNode> nodes,
        List<String> openQuestions,
        Map<String, Object> diagnostics
) {
    public record DecisionNode(
            String code,
            String title,
            String source,
            String confidenceBand,
            List<String> inputs,
            List<String> outputs,
            List<String> risks
    ) {
    }
}
