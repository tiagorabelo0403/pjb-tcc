package com.tcc.pjb.backend.model.dto.jobs;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String type,
        String status,
        int priority,
        String inboxKey,
        String ownerUserId,
        String idempotencyKey,
        long progressCurrent,
        long progressTotal,
        int attempts,
        int maxAttempts,
        Instant nextRetryAt,
        String lastError,
        String lockedBy,
        Instant lockedAt,
        Instant pausedAt,
        String pauseReason,
        Instant createdAt,
        Instant updatedAt
) {
}
