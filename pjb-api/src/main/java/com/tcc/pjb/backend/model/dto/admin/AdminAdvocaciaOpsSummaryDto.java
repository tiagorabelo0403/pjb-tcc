package com.tcc.pjb.backend.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public final class AdminAdvocaciaOpsSummaryDto {

    private AdminAdvocaciaOpsSummaryDto() {
    }

    public record ClienteStats(
            long total,
            long semCpfHash,
            long emAnalise,
            Map<String, Long> porStatus
    ) {
    }

    public record BackfillRunLite(
            String jobId,
            String type,
            String inboxKey,
            String requestedBy,
            int batchSize,
            boolean dryRun,
            long afterId,
            Long untilId,
            @Schema(description = "Data/hora de início do run", format = "date-time",
                    example = "2026-06-01T10:00:00-03:00") String startedAt,
            @Schema(description = "Data/hora de conclusão do run", format = "date-time",
                    example = "2026-06-01T10:05:00-03:00") String finishedAt,
            long processed,
            long updated,
            long duplicates,
            long lastCursor,
            String lastError
    ) {
    }

    public record OpsSummaryResponse(
            ClienteStats clientes,
            BackfillRunLite ultimoBackfill
    ) {
    }
}
