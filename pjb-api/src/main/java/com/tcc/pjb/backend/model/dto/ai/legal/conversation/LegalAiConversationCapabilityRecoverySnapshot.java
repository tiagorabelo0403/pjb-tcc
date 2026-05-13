package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationCapabilityRecoverySnapshot(
        String status,
        boolean recoveryEligible,
        boolean capabilityRecovered,
        String recoveryLane,
        List<String> recoveryCandidateToolIds,
        List<String> unmetRequirements,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("recoveryEligible", recoveryEligible);
        out.put("capabilityRecovered", capabilityRecovered);
        out.put("recoveryLane", recoveryLane);
        out.put("recoveryCandidateToolIds", recoveryCandidateToolIds == null ? List.of() : List.copyOf(recoveryCandidateToolIds));
        out.put("unmetRequirements", unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
