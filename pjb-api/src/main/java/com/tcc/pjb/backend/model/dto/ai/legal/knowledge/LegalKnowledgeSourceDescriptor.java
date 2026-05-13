package com.tcc.pjb.backend.model.dto.ai.legal.knowledge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalKnowledgeSourceDescriptor(
        String sourceId,
        String title,
        String sourceKind,
        String authorityLevel,
        String institution,
        String storageLane,
        String licensingModel,
        String baseUrl,
        String refreshStrategy,
        List<String> branches,
        List<String> artifactTypes,
        List<String> retrievalTags,
        List<String> restrictions
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("sourceId", sourceId);
        out.put("title", title);
        out.put("sourceKind", sourceKind);
        out.put("authorityLevel", authorityLevel);
        out.put("institution", institution);
        out.put("storageLane", storageLane);
        out.put("licensingModel", licensingModel);
        out.put("baseUrl", baseUrl);
        out.put("refreshStrategy", refreshStrategy);
        out.put("branches", branches == null ? List.of() : List.copyOf(branches));
        out.put("artifactTypes", artifactTypes == null ? List.of() : List.copyOf(artifactTypes));
        out.put("retrievalTags", retrievalTags == null ? List.of() : List.copyOf(retrievalTags));
        out.put("restrictions", restrictions == null ? List.of() : List.copyOf(restrictions));
        return Collections.unmodifiableMap(out);
    }
}
