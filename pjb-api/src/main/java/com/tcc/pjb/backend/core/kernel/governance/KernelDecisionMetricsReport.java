package com.tcc.pjb.backend.core.kernel.governance;

import java.util.List;
import java.util.Map;

public record KernelDecisionMetricsReport(
        String scope,
        String status,
        double confidence,
        long totalDecisions,
        long blockedDecisions,
        long approvalRequiredDecisions,
        long internalDraftDecisions,
        long last24hDecisions,
        List<String> hotSignals,
        List<String> stabilitySignals,
        Map<String, Object> diagnostics
) {
}
