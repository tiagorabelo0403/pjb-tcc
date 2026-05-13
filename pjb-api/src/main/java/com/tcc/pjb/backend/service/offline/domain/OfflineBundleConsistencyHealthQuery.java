package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleConsistencyHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
