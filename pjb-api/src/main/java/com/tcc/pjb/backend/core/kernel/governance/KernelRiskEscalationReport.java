package com.tcc.pjb.backend.core.kernel.governance;

import java.util.List;
import java.util.Map;

public record KernelRiskEscalationReport(
        String scope,
        String status,
        double confidence,
        String escalationLevel,
        List<String> containmentActions,
        List<String> escalationTriggers,
        List<String> recommendedLanes,
        Map<String, Object> diagnostics
) {
}
