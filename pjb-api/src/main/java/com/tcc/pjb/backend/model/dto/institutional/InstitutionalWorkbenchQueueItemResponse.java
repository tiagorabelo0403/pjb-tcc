package com.tcc.pjb.backend.model.dto.institutional;

import java.time.Instant;
import java.util.List;

public record InstitutionalWorkbenchQueueItemResponse(
        Long workItemId,
        Long processoId,
        String numeroProcesso,
        String titulo,
        String queueCode,
        String status,
        Integer prioridade,
        Instant dueAt,
        InstitutionalWorkbenchActionResponse primaryAction,
        List<InstitutionalWorkbenchActionResponse> allowedActions,
        List<InstitutionalWorkbenchActionResponse> blockedActions,
        InstitutionalWorkbenchExplainabilityResponse explainability
) {
}
