package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailAnalyticsGranularityStatusResponse(
        String granularity,
        long persistedBucketCount,
        Instant oldestBucketStartedAt,
        Instant newestBucketStartedAt
) {
    public PjbAuthorizationTrailAnalyticsGranularityStatusResponse {
        granularity = granularity == null || granularity.isBlank() ? "DAY" : granularity;
    }
}
