package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record NegotiationApprovalMatrixReport(
        String scope,
        String status,
        double confidence,
        String approvalBand,
        String releaseMode,
        List<String> approvalGates,
        List<String> escalationLanes,
        List<String> internalControls,
        List<String> releaseChecklist,
        Map<String, Object> diagnostics
) {
}
