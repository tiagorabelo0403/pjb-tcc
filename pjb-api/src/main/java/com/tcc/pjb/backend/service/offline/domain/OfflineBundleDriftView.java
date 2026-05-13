package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleDriftView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
