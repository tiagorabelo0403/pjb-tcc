package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalMcpServerDescriptor(
        String serverId,
        String label,
        String domain,
        String transportMode,
        String authorizationMode,
        boolean batchingEnabled,
        boolean completionsEnabled,
        boolean rootsAware,
        boolean samplingAware,
        String trustProfile,
        List<String> prompts,
        List<String> resources,
        List<String> capabilityLanes,
        List<LegalMcpToolDescriptor> tools
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("serverId", serverId);
        out.put("label", label);
        out.put("domain", domain);
        out.put("transportMode", transportMode);
        out.put("authorizationMode", authorizationMode);
        out.put("batchingEnabled", batchingEnabled);
        out.put("completionsEnabled", completionsEnabled);
        out.put("rootsAware", rootsAware);
        out.put("samplingAware", samplingAware);
        out.put("trustProfile", trustProfile);
        out.put("prompts", prompts == null ? List.of() : List.copyOf(prompts));
        out.put("resources", resources == null ? List.of() : List.copyOf(resources));
        out.put("capabilityLanes", capabilityLanes == null ? List.of() : List.copyOf(capabilityLanes));
        out.put("tools", tools == null ? List.of() : tools.stream().map(LegalMcpToolDescriptor::asMap).toList());
        return Collections.unmodifiableMap(out);
    }
}
