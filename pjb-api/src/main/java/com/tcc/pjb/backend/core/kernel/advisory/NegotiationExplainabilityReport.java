package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record NegotiationExplainabilityReport(
        String scope,
        String status,
        double confidence,
        List<NegotiationNode> nodes,
        List<String> openQuestions,
        Map<String, Object> diagnostics
) {
    public record NegotiationNode(
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
