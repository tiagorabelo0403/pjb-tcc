package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalMcpDoctorReport(
        String doctorId,
        String status,
        boolean ready,
        String operationalMode,
        List<LegalMcpDoctorCheck> checks,
        List<String> warnings,
        List<String> blockers
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("doctorId", doctorId);
        out.put("status", status);
        out.put("ready", ready);
        out.put("operationalMode", operationalMode);
        out.put("checks", checks == null ? List.of() : checks.stream().map(LegalMcpDoctorCheck::asMap).toList());
        out.put("warnings", warnings == null ? List.of() : List.copyOf(warnings));
        out.put("blockers", blockers == null ? List.of() : List.copyOf(blockers));
        return Collections.unmodifiableMap(out);
    }
}
