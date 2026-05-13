package com.tcc.pjb.backend.model.dto.processual.runtime.guard;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessualOperationGuardResponse(
        Long processoId,
        String operationCode,
        String idempotencyScope,
        String idempotencyKey,
        String idempotencyDecision,
        String idempotencyStatus,
        boolean accepted,
        String circuitState,
        String resourceType,
        String resourceId,
        String responseJson,
        String outboxEventId,
        List<String> warnings,
        Map<String, Object> metadata,
        Instant processedAt
) {
    public ProcessualOperationGuardResponse {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        processedAt = processedAt == null ? Instant.now() : processedAt;
    }
}
