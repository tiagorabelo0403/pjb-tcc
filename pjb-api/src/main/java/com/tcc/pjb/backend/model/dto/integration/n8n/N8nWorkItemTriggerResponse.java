package com.tcc.pjb.backend.model.dto.integration.n8n;

import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationResponse;
import java.time.Instant;

public record N8nWorkItemTriggerResponse(
        String requestId,
        String traceId,
        String workflowKey,
        Instant processedAt,
        WorkItemGenerationResponse result
) {
}
