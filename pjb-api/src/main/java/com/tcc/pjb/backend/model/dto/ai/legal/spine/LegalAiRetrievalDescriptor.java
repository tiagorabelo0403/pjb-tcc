package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiRetrievalDescriptor(
        String pipelineCode,
        List<String> stages,
        boolean graphAware,
        boolean multimodalEnabled,
        Map<String, Object> retrievalPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "pipelineCode", pipelineCode,
                "stages", stages == null ? List.of() : List.copyOf(stages),
                "graphAware", graphAware,
                "multimodalEnabled", multimodalEnabled,
                "retrievalPolicy", retrievalPolicy == null ? Map.of() : Map.copyOf(retrievalPolicy)
        );
    }
}
