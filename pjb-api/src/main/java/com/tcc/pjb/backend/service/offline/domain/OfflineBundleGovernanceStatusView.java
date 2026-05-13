package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleGovernanceStatusView(
        String reference,
        String status,
        String summary
) {
}
