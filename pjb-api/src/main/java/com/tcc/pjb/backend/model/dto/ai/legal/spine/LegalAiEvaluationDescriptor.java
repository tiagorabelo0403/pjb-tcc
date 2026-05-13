package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiEvaluationDescriptor(
        List<String> evalSuites,
        List<String> benchmarkFamilies,
        boolean replayEnabled,
        Map<String, Object> evaluationPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "evalSuites", evalSuites == null ? List.of() : List.copyOf(evalSuites),
                "benchmarkFamilies", benchmarkFamilies == null ? List.of() : List.copyOf(benchmarkFamilies),
                "replayEnabled", replayEnabled,
                "evaluationPolicy", evaluationPolicy == null ? Map.of() : Map.copyOf(evaluationPolicy)
        );
    }
}
