package com.tcc.pjb.backend.service.offline.domain;

public record OfflineManifestAuditView(
        String reference,
        String status,
        String summary
) {
}
