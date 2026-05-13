package com.tcc.pjb.backend.model.dto.ai.legal.mesh;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiMeshProfileResponse(
        String profileCode,
        String version,
        String capability,
        List<String> qualityFilters,
        List<String> memoryScopes,
        Map<String, Object> rag,
        Map<String, Object> mcp,
        Map<String, Object> runtime,
        Map<String, Object> versions,
        Map<String, Object> legalDepth,
        List<LegalAiToolDescriptor> tools
) {
    public LegalAiMeshProfileResponse {
        qualityFilters = qualityFilters == null ? List.of() : List.copyOf(qualityFilters);
        memoryScopes = memoryScopes == null ? List.of() : List.copyOf(memoryScopes);
        rag = rag == null ? Map.of() : Map.copyOf(rag);
        mcp = mcp == null ? Map.of() : Map.copyOf(mcp);
        runtime = runtime == null ? Map.of() : Map.copyOf(runtime);
        versions = versions == null ? Map.of() : Map.copyOf(versions);
        legalDepth = legalDepth == null ? Map.of() : Map.copyOf(legalDepth);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("profileCode", profileCode);
        out.put("version", version);
        out.put("capability", capability);
        out.put("qualityFilters", qualityFilters);
        out.put("memoryScopes", memoryScopes);
        out.put("rag", rag);
        out.put("mcp", mcp);
        out.put("runtime", runtime);
        out.put("versions", versions);
        out.put("legalDepth", legalDepth);
        out.put("tools", tools.stream().map(tool -> Map.of(
                "id", tool.id(),
                "label", tool.label(),
                "category", tool.category(),
                "readOnly", tool.readOnly(),
                "mcpEnabled", tool.mcpEnabled(),
                "ragAware", tool.ragAware(),
                "requiresStepUp", tool.requiresStepUp(),
                "sourceLane", tool.sourceLane()
        )).toList());
        return Collections.unmodifiableMap(out);
    }
}
