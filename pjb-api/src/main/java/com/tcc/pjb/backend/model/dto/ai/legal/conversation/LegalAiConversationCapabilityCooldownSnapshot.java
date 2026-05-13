package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationCapabilityCooldownSnapshot(
        String status,
        boolean lockActive,
        String lockScope,
        String lockKey,
        int cooldownTurnsRemaining,
        boolean blockedCapability,
        List<String> blockedToolIds,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("lockActive", lockActive);
        out.put("lockScope", lockScope);
        out.put("lockKey", lockKey);
        out.put("cooldownTurnsRemaining", cooldownTurnsRemaining);
        out.put("blockedCapability", blockedCapability);
        out.put("blockedToolIds", blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
