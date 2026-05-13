package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.util.List;
import java.util.Map;

public record LaianeJudicialDecisionAdvisoryResponse(
        JudicialDecisionTemplateCode templateCode,
        String advisoryMode,
        boolean reviewRequired,
        boolean publicationLocked,
        String rationaleSummary,
        List<String> legalAnchors,
        List<String> reasoningChecklist,
        List<String> pendingFacts,
        String dispositiveBase,
        Map<String, String> fillableVariables,
        Map<String, Object> metadata
) {
    public LaianeJudicialDecisionAdvisoryResponse {
        legalAnchors = legalAnchors == null ? List.of() : List.copyOf(legalAnchors);
        reasoningChecklist = reasoningChecklist == null ? List.of() : List.copyOf(reasoningChecklist);
        pendingFacts = pendingFacts == null ? List.of() : List.copyOf(pendingFacts);
        fillableVariables = fillableVariables == null ? Map.of() : Map.copyOf(fillableVariables);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Map<String, Object> toMap() {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("templateCode", templateCode != null ? templateCode.name() : null);
        out.put("advisoryMode", advisoryMode);
        out.put("reviewRequired", reviewRequired);
        out.put("publicationLocked", publicationLocked);
        out.put("rationaleSummary", rationaleSummary);
        out.put("legalAnchors", legalAnchors);
        out.put("reasoningChecklist", reasoningChecklist);
        out.put("pendingFacts", pendingFacts);
        out.put("dispositiveBase", dispositiveBase);
        out.put("fillableVariables", fillableVariables);
        out.put("metadata", metadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return java.util.Map.copyOf(out);
    }
}
