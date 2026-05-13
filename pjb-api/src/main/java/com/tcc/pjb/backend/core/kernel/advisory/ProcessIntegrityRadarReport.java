package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record ProcessIntegrityRadarReport(
        String status,
        double score,
        boolean blocking,
        List<Finding> findings,
        List<String> nextActions,
        List<String> watchpoints,
        Map<String, Object> diagnostics
) {
    public record Finding(
            String code,
            String domain,
            String title,
            String severity,
            boolean blocking,
            String message,
            List<String> evidence
    ) {
    }
}
