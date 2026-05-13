package com.tcc.pjb.backend.model.dto.secretariat.governance;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecretariatCoverageSnapshotDto(
        Instant generatedAt,
        String inboxKey,
        String coverageMode,
        Map<String, Object> metrics,
        List<Cell> cells,
        List<String> gaps,
        List<String> warnings,
        Map<String, Object> routes
) {
    public record Cell(
            String cellCode,
            String cellLabel,
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
