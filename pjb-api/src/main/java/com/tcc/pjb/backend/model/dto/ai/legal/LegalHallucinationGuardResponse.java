package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.List;
import java.util.Map;

public record LegalHallucinationGuardResponse(
        String profileCode,
        String version,
        String capability,
        String status,
        boolean articleReferenceVerificationRequired,
        boolean precedentVerificationRequired,
        boolean freeFormCitationBlocked,
        String citationEmissionMode,
        String unresolvedCitationPlaceholder,
        String evidenceProvenanceStatus,
        String evidenceProvenanceTier,
        String sovereignProvenanceMode,
        String groundingPromotionStatus,
        List<String> suspiciousSignals,
        List<String> blockedReasons,
        Map<String, Object> trace
) {
}
