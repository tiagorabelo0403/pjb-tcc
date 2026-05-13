package com.tcc.pjb.backend.model.dto.institutional;

import java.time.Instant;
import java.util.List;

public record InstitutionalWorkbenchQuickActionsResponse(
        Instant generatedAt,
        String actorClass,
        Long processoId,
        String numeroProcesso,
        List<InstitutionalWorkbenchActionResponse> actions,
        List<String> warnings
) {
}
