package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleDecisionHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
