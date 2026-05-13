package com.tcc.pjb.backend.service.offline.domain;

public record OfflineSyncWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
