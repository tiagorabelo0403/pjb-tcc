package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationCapabilityRecurrenceSnapshot(
        String status,
        boolean recurrenceDetected,
        boolean processScoped,
        String registryKey,
        int recurrenceCount,
        int failedRehabilitationCount,
        boolean repeatedDriftDetected,
        int quarantineHitCount,
        String riskTier,
        String escalationMode,
        List<String> blockedToolIds,
        List<String> unmetRequirements,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("recurrenceDetected", recurrenceDetected);
        out.put("processScoped", processScoped);
        out.put("registryKey", registryKey);
        out.put("recurrenceCount", recurrenceCount);
        out.put("failedRehabilitationCount", failedRehabilitationCount);
        out.put("repeatedDriftDetected", repeatedDriftDetected);
        out.put("quarantineHitCount", quarantineHitCount);
        out.put("riskTier", riskTier);
        out.put("escalationMode", escalationMode);
        out.put("blockedToolIds", blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds));
        out.put("unmetRequirements", unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
