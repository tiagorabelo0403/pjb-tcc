package com.tcc.pjb.backend.model.dto.ai.legal.knowledge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalKnowledgeCoverageSnapshot(
        String status,
        String coverageMode,
        List<String> matchedBranches,
        List<LegalKnowledgeSourceDescriptor> officialSources,
        List<LegalKnowledgeSourceDescriptor> doctrineSources,
        List<String> priorityOrder,
        List<String> ingestionPolicies,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("coverageMode", coverageMode);
        out.put("matchedBranches", matchedBranches == null ? List.of() : List.copyOf(matchedBranches));
        out.put("officialSources", officialSources == null ? List.of() : officialSources.stream().map(LegalKnowledgeSourceDescriptor::asMap).toList());
        out.put("doctrineSources", doctrineSources == null ? List.of() : doctrineSources.stream().map(LegalKnowledgeSourceDescriptor::asMap).toList());
        out.put("priorityOrder", priorityOrder == null ? List.of() : List.copyOf(priorityOrder));
        out.put("ingestionPolicies", ingestionPolicies == null ? List.of() : List.copyOf(ingestionPolicies));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
