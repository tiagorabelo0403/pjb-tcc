package com.tcc.pjb.backend.model.dto.institutional;

import java.time.Instant;
import java.util.List;

public record InstitutionalWorkbenchActionPreviewResponse(
        Instant generatedAt,
        String actorClass,
        Long processoId,
        String numeroProcesso,
        InstitutionalWorkbenchActionResponse action,
        InstitutionalWorkbenchExplainabilityResponse explainability,
        List<String> warnings
) {
    public InstitutionalWorkbenchActionPreviewResponse {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
