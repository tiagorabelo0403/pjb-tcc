package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleWindowAuditView(
        String reference,
        String status,
        String summary
) {
}
