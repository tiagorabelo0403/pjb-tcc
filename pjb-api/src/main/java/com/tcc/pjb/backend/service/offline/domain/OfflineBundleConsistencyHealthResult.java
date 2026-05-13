package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleConsistencyHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
