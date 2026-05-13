package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record NegotiationMemoryReport(
        String scope,
        String status,
        double confidence,
        List<String> learnedPatterns,
        List<String> repeatedFailureModes,
        List<String> reusablePlaybooks,
        List<String> cautionPoints,
        List<String> negotiationKeys,
        Map<String, Object> diagnostics
) {
}
