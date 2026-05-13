package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record InstitutionalGovernanceContextReport(
        String scope,
        String status,
        double confidence,
        List<String> anchorDimensions,
        List<String> governanceAlerts,
        List<String> policyGuards,
        List<String> escalationPlaybooks,
        List<String> governanceKeys,
        Map<String, Object> diagnostics
) {
}
