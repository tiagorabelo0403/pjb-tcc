package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import java.util.List;

public record AdminInstitutionalPublicRecognitionResponse(
        String policyVersion,
        Instant evaluatedAt,
        String scopeCode,
        String scopeLabel,
        String statusCode,
        String statusLabel,
        boolean recognized,
        boolean autoActivatable,
        boolean humanReviewRequired,
        List<String> acceptedOfficialSources,
        List<EvidenceRule> requiredEvidence,
        List<String> guarantees,
        List<String> reasons,
        List<String> blockers,
        List<String> nextSafeSteps
) {

    public record EvidenceRule(
            String code,
            String label,
            boolean mandatoryForAutomaticActivation,
            boolean satisfied,
            String sourceType
    ) {
    }
}
