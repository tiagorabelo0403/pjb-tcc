package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Map;

public record LegalMcpToolDescriptor(
        String toolId,
        String label,
        String toolClass,
        boolean readOnly,
        boolean requiresStepUp,
        String evidenceLane,
        String riskLevel,
        Map<String, Object> annotations
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "toolId", toolId,
                "label", label,
                "toolClass", toolClass,
                "readOnly", readOnly,
                "requiresStepUp", requiresStepUp,
                "evidenceLane", evidenceLane,
                "riskLevel", riskLevel,
                "annotations", annotations == null ? Map.of() : Map.copyOf(annotations)
        );
    }
}
