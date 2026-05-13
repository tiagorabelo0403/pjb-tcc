package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleExpiryQuery(
        String bundleToken,
        Instant referenceTime
) {}
