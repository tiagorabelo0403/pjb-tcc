package com.tcc.pjb.backend.core.kernel.governance;

import java.util.List;
import java.util.Map;

public record NegotiationMessageDecision(
        String scope,
        String status,
        String decisionCode,
        double confidence,
        boolean releaseAllowed,
        boolean approvalRequired,
        boolean internalDraftRequired,
        boolean strictMode,
        String approvalBand,
        String releaseMode,
        String policyTier,
        String riskLevel,
        List<String> reasons,
        List<String> mandatoryActions,
        String releaseMessage,
        Map<String, Object> diagnostics
) {
}
