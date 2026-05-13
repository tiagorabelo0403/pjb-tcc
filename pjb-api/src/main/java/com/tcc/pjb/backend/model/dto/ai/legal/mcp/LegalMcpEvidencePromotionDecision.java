package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalMcpEvidencePromotionDecision(
        String decisionId,
        String status,
        boolean replayReady,
        double evidenceScore,
        String approvalLane,
        List<String> promotedToolExampleIds,
        List<String> reasons,
        List<String> safeguards
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("decisionId", decisionId);
        out.put("status", status);
        out.put("replayReady", replayReady);
        out.put("evidenceScore", evidenceScore);
        out.put("approvalLane", approvalLane);
        out.put("promotedToolExampleIds", promotedToolExampleIds == null ? List.of() : List.copyOf(promotedToolExampleIds));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("safeguards", safeguards == null ? List.of() : List.copyOf(safeguards));
        return Collections.unmodifiableMap(out);
    }
}
