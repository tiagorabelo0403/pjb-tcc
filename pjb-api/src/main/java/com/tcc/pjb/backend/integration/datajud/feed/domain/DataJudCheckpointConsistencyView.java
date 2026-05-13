package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCheckpointConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
