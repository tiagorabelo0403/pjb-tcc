package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleExpiryView(
        Long bundleId,
        Instant expiresAt,
        boolean expired
) {}
