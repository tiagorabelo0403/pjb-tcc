package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiHallucinationGuardDescriptor(
        boolean articleReferenceVerificationRequired,
        boolean precedentVerificationRequired,
        boolean freeFormCitationBlocked,
        boolean ungroundedNormativeClaimsBlocked,
        String citationEmissionMode,
        String unresolvedCitationPlaceholder,
        List<String> authoritySources,
        List<String> suspiciousPatterns,
        Map<String, Object> hallucinationPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "articleReferenceVerificationRequired", articleReferenceVerificationRequired,
                "precedentVerificationRequired", precedentVerificationRequired,
                "freeFormCitationBlocked", freeFormCitationBlocked,
                "ungroundedNormativeClaimsBlocked", ungroundedNormativeClaimsBlocked,
                "citationEmissionMode", citationEmissionMode,
                "unresolvedCitationPlaceholder", unresolvedCitationPlaceholder,
                "authoritySources", authoritySources == null ? List.of() : List.copyOf(authoritySources),
                "suspiciousPatterns", suspiciousPatterns == null ? List.of() : List.copyOf(suspiciousPatterns),
                "hallucinationPolicy", hallucinationPolicy == null ? Map.of() : Map.copyOf(hallucinationPolicy)
        );
    }
}
