package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.List;
import java.util.Map;

public record EvidenceSufficiencyPlan(
        List<String> missingDataRequests,
        List<String> suggestedQueryExpansions,
        Map<String, Object> meta
) {
    public EvidenceSufficiencyPlan {
        missingDataRequests = (missingDataRequests == null) ? List.of() : List.copyOf(missingDataRequests);
        suggestedQueryExpansions = (suggestedQueryExpansions == null) ? List.of() : List.copyOf(suggestedQueryExpansions);
        meta = (meta == null) ? Map.of() : Map.copyOf(meta);
    }
}
