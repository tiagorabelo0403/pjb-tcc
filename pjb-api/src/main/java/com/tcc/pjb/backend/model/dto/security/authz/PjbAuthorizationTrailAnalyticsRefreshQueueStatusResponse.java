package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse(
        boolean workerEnabled,
        int configuredBatchSize,
        int processingTimeoutSeconds,
        int completionRetentionDays,
        int pendingCount,
        int processingCount,
        int failedCount,
        int completedCount,
        int staleProcessingCount,
        int completedEligibleForCleanupCount,
        Instant oldestPendingBucket,
        Instant newestCompletedAt,
        Instant newestFailureAt
) {
}
