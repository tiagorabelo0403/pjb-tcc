package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record KernelOperationalGovernanceReport(
        String scope,
        String status,
        double confidence,
        List<String> risks,
        List<String> controls,
        List<String> nextActions,
        List<String> watchpoints,
        Map<String, Object> diagnostics
) {
}
