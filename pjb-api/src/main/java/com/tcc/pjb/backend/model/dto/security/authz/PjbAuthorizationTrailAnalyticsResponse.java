package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;
import java.util.List;

public record PjbAuthorizationTrailAnalyticsResponse(
        String granularity,
        Instant windowStartedAt,
        Instant windowEndedAtExclusive,
        Instant lastMaterializedAt,
        long persistedBucketCount,
        long representedEvents,
        List<PjbAuthorizationTrailTimeBucketResponse> timeSeries,
        List<PjbAuthorizationTrailAnalyticsDimensionResponse> dimensions
) {
    public PjbAuthorizationTrailAnalyticsResponse {
        granularity = granularity == null || granularity.isBlank() ? "DAY" : granularity;
        timeSeries = timeSeries == null ? List.of() : List.copyOf(timeSeries);
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
    }
}
