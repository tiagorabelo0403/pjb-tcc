package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleConsistencyView(
        String bundleToken,
        String status,
        boolean consistent,
        Instant evaluatedAt,
        String summary
) {}
