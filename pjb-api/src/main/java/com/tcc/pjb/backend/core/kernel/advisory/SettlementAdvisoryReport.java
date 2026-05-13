package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record SettlementAdvisoryReport(
        String status,
        double score,
        boolean executable,
        NegotiationWindowReport window,
        List<String> conditionalClauses,
        List<String> executionSafeguards,
        List<String> nextMoves,
        Map<String, Object> diagnostics
) {
}
