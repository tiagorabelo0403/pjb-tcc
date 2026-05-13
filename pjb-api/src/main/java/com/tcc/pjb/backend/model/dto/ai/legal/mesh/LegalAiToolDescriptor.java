package com.tcc.pjb.backend.model.dto.ai.legal.mesh;

public record LegalAiToolDescriptor(
        String id,
        String label,
        String category,
        boolean readOnly,
        boolean mcpEnabled,
        boolean ragAware,
        boolean requiresStepUp,
        String sourceLane
) {
}
