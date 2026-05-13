package com.tcc.pjb.backend.model.dto.institutional.support.operations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstitutionalSupportCoverageSnapshotResponse(
        Instant generatedAt,
        Map<String, Object> lane,
        Map<String, Object> metrics,
        List<Cell> cells,
        List<String> gaps,
        List<String> warnings,
        Map<String, Object> routes
) {
    public record Cell(
            String ownerCode,
            String ownerLabel,
            long totalItems,
            long overdueItems,
            long blockingItems,
            long unassignedItems,
            String loadBand,
            Instant nextDueAt,
            List<String> substitutePool,
            List<String> redistributionSuggestions,
            Map<String, Object> metrics
    ) {
    }
}
