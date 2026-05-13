package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationTrustZoneSnapshot(
        String status,
        String trustZone,
        boolean sovereignBoundaryRequired,
        boolean processScoped,
        String sourceZone,
        String attachmentZone,
        String capabilityZone,
        String trustZoneMode,
        List<String> blockedToolIds,
        List<String> elevatedStepUpToolIds,
        List<String> unmetRequirements,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("trustZone", trustZone);
        out.put("sovereignBoundaryRequired", sovereignBoundaryRequired);
        out.put("processScoped", processScoped);
        out.put("sourceZone", sourceZone);
        out.put("attachmentZone", attachmentZone);
        out.put("capabilityZone", capabilityZone);
        out.put("trustZoneMode", trustZoneMode);
        out.put("blockedToolIds", blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds));
        out.put("elevatedStepUpToolIds", elevatedStepUpToolIds == null ? List.of() : List.copyOf(elevatedStepUpToolIds));
        out.put("unmetRequirements", unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
