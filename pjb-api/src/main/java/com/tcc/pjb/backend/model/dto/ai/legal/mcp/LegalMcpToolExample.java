package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record LegalMcpToolExample(
        String exampleId,
        String toolId,
        String title,
        String usagePattern,
        String invocationTemplate,
        String safeWhen
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("exampleId", exampleId);
        out.put("toolId", toolId);
        out.put("title", title);
        out.put("usagePattern", usagePattern);
        out.put("invocationTemplate", invocationTemplate);
        out.put("safeWhen", safeWhen);
        return Collections.unmodifiableMap(out);
    }
}
