package com.tcc.pjb.backend.service.offline.domain;

public record OfflineReplayHealthView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
