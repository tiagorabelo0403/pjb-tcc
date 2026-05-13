package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleConsistencyQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
