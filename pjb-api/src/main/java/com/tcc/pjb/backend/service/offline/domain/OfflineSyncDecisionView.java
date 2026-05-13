package com.tcc.pjb.backend.service.offline.domain;

public record OfflineSyncDecisionView(
        String bundleToken,
        String status,
        boolean conflict,
        String decision
) {}
