package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record ProtocolDryRunReport(
        String status,
        boolean apto,
        List<Check> checks,
        List<String> nextActions,
        Map<String, Object> diagnostics
) {
    public List<Finding> findings() {
        return checks == null ? java.util.List.of() : checks.stream().map(Finding::fromCheck).toList();
    }

    public record Check(
            String code,
            String title,
            String severity,
            boolean passed,
            String message
    ) {
    }

    public record Finding(String code, String title, String severity, boolean blocking, String message) {
        static Finding fromCheck(Check c) {
            return new Finding(c.code(), c.title(), c.severity(), !c.passed(), c.message());
        }
    }
}
