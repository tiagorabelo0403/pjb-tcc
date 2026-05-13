package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record LegalMcpDoctorCheck(
        String checkId,
        String label,
        String status,
        boolean blocking,
        String details
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("checkId", checkId);
        out.put("label", label);
        out.put("status", status);
        out.put("blocking", blocking);
        out.put("details", details);
        return Collections.unmodifiableMap(out);
    }
}
