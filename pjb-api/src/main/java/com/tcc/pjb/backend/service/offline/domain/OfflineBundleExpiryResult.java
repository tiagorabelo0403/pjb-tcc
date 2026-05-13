package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleExpiryResult(
        String bundleToken,
        Instant expiresAt,
        boolean expired,
        long remainingSeconds
) {}
