package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record NegotiationChannelGovernanceReport(
        String scope,
        String status,
        double confidence,
        String operatingMode,
        String persistenceMode,
        String approvalHandshake,
        List<String> participantDirectives,
        List<String> releaseBoundaries,
        List<String> auditDirectives,
        List<String> memoryDirectives,
        List<String> deliveryGuardrails,
        List<String> fallbackLanes,
        Map<String, Object> diagnostics
) {
}
