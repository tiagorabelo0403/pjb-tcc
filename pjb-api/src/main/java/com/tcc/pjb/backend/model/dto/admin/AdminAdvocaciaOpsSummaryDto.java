package com.tcc.pjb.backend.model.dto.admin;

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
            String startedAt,
            String finishedAt,
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
