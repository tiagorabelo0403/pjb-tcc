package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationMemorySnapshot(
        String conversationId,
        String processoId,
        String userProfile,
        List<Map<String, Object>> retainedTurns,
        Map<String, Object> scopedMemory,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("conversationId", conversationId);
        out.put("processoId", processoId);
        out.put("userProfile", userProfile);
        out.put("retainedTurns", retainedTurns == null ? List.of() : retainedTurns.stream().map(Map::copyOf).toList());
        out.put("scopedMemory", scopedMemory == null ? Map.of() : Map.copyOf(scopedMemory));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
