package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationDocumentSecuritySnapshot(
        String status,
        List<String> allowlistedSources,
        List<String> blockedSources,
        List<String> allowedAttachments,
        List<String> quarantinedAttachments,
        List<String> alerts,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("allowlistedSources", allowlistedSources == null ? List.of() : List.copyOf(allowlistedSources));
        out.put("blockedSources", blockedSources == null ? List.of() : List.copyOf(blockedSources));
        out.put("allowedAttachments", allowedAttachments == null ? List.of() : List.copyOf(allowedAttachments));
        out.put("quarantinedAttachments", quarantinedAttachments == null ? List.of() : List.copyOf(quarantinedAttachments));
        out.put("alerts", alerts == null ? List.of() : List.copyOf(alerts));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
