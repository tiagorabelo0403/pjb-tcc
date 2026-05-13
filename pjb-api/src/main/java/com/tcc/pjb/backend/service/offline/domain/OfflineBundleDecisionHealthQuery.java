package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleDecisionHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
