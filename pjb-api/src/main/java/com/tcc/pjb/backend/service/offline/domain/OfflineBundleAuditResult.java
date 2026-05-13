package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleAuditResult(
        OfflineBundleManifestSnapshot manifest,
        OfflineBundleReplaySnapshot replay,
        OfflineBundleSyncSnapshot sync
) {}
