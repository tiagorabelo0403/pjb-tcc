package com.tcc.pjb.backend.model.dto.admin.backfill;

import java.util.UUID;

public record AdminBackfillStatusResponse(
        UUID jobId,
        String jobStatus,
        long progressCurrent,
        long progressTotal,
        long processed,
        long updated,
        long duplicates,
        long lastCursor,
        boolean dryRun,
        long afterId,
        Long untilId,
        int batchSize,
        String startedAt,
        String finishedAt,
        String lastError
) {
}
