package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleManifestView(
        String bundleToken,
        String manifestHash,
        String escopo,
        String status
) {}
