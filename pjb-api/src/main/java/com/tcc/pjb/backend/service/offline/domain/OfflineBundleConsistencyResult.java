package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleConsistencyResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
