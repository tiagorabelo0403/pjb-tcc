package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalMcpSkillDescriptor(
        String skillId,
        String label,
        String category,
        String activationMode,
        boolean sensitive,
        List<String> supportedCapabilities,
        List<String> preferredServerIds,
        List<String> preferredToolIds
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("skillId", skillId);
        out.put("label", label);
        out.put("category", category);
        out.put("activationMode", activationMode);
        out.put("sensitive", sensitive);
        out.put("supportedCapabilities", supportedCapabilities == null ? List.of() : List.copyOf(supportedCapabilities));
        out.put("preferredServerIds", preferredServerIds == null ? List.of() : List.copyOf(preferredServerIds));
        out.put("preferredToolIds", preferredToolIds == null ? List.of() : List.copyOf(preferredToolIds));
        return Collections.unmodifiableMap(out);
    }
}
