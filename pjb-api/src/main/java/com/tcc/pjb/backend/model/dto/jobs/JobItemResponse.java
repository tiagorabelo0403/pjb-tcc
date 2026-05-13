package com.tcc.pjb.backend.model.dto.jobs;

import java.time.Instant;
import java.util.UUID;

public record JobItemResponse(
        UUID id,
        String itemKey,
        String status,
        int attempts,
        int maxAttempts,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
