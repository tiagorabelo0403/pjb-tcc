package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationCapabilityRehabilitationSnapshot(
        String status,
        boolean rehabilitationRequired,
        boolean releaseEligible,
        boolean capabilityReleased,
        String releaseLane,
        int stableWinningTurns,
        int requiredStableTurns,
        int rehabilitationWindowTurnsRemaining,
        List<String> releasedToolIds,
        List<String> blockedToolIds,
        List<String> unmetRequirements,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("rehabilitationRequired", rehabilitationRequired);
        out.put("releaseEligible", releaseEligible);
        out.put("capabilityReleased", capabilityReleased);
        out.put("releaseLane", releaseLane);
        out.put("stableWinningTurns", stableWinningTurns);
        out.put("requiredStableTurns", requiredStableTurns);
        out.put("rehabilitationWindowTurnsRemaining", rehabilitationWindowTurnsRemaining);
        out.put("releasedToolIds", releasedToolIds == null ? List.of() : List.copyOf(releasedToolIds));
        out.put("blockedToolIds", blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds));
        out.put("unmetRequirements", unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
