package com.tcc.pjb.backend.model.dto.admin.backfill;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "Data/hora de início do job de backfill", format = "date-time",
                example = "2026-06-01T10:00:00-03:00") String startedAt,
        @Schema(description = "Data/hora de conclusão do job de backfill", format = "date-time",
                example = "2026-06-01T10:05:00-03:00") String finishedAt,
        String lastError
) {
}
