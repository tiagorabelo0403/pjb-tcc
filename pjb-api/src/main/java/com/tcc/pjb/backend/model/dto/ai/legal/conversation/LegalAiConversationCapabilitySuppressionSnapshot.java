package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationCapabilitySuppressionSnapshot(
        String status,
        boolean suppressionDetected,
        boolean processScoped,
        String suppressionScope,
        String processClass,
        String sigiloLevel,
        String policyTier,
        String suppressionMode,
        List<String> blockedToolIds,
        List<String> elevatedStepUpToolIds,
        List<String> unmetRequirements,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("suppressionDetected", suppressionDetected);
        out.put("processScoped", processScoped);
        out.put("suppressionScope", suppressionScope);
        out.put("processClass", processClass);
        out.put("sigiloLevel", sigiloLevel);
        out.put("policyTier", policyTier);
        out.put("suppressionMode", suppressionMode);
        out.put("blockedToolIds", blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds));
        out.put("elevatedStepUpToolIds", elevatedStepUpToolIds == null ? List.of() : List.copyOf(elevatedStepUpToolIds));
        out.put("unmetRequirements", unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
