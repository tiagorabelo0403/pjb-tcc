package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.Map;

public record CalculoJudicialExperiencePreferenceResponse(
        String resolvedExperienceMode,
        String source,
        boolean teamScoped,
        boolean domainScoped,
        boolean institutionalPolicyApplied,
        Long equipeAtivaId,
        String domainCode,
        String principalKey,
        Map<String, Object> selector,
        Map<String, Object> policyContext,
        Instant updatedAt
) {
}
