package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record ContextualPrecedentAdvisoryReport(
        String scope,
        String status,
        double adherence,
        List<String> anchorDimensions,
        List<String> recommendedQueries,
        List<String> targetDecisionProfiles,
        List<String> narrativeAngles,
        List<String> cautionPoints,
        Map<String, Object> diagnostics
) {
}
