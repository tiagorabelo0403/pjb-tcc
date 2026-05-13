package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse(
        String operation,
        String granularity,
        Instant windowStartedAt,
        Instant windowEndedAtExclusive,
        int touchedBuckets,
        int processedBuckets,
        int failedBuckets,
        int pendingBuckets,
        Instant executedAt
) {
}
