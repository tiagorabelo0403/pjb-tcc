package com.tcc.pjb.backend.model.dto.workitem;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkItemGenerationResponse(
        String requestId,
        Instant generatedAt,
        Long processoId,
        int created,
        int skipped,
        List<WorkItemDto> createdItems,
        Map<String, Object> debug
) {
    public List<WorkItemDto> items() {
        return createdItems;
    }
}
