package com.tcc.pjb.backend.model.dto.admin.backfill;

import jakarta.validation.constraints.Min;

public record AdminBackfillMniMigrationRequest(
        @Min(1) Integer batchSize,
        @Min(0) Long afterId,
        Long untilId,
        @Min(0) Integer priority,
        @Min(1) Integer maxAttempts,
        String inboxKey
) {
}
