package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailAnalyticsMaterializationResponse(
        String granularity,
        Instant windowStartedAt,
        Instant windowEndedAtExclusive,
        int sourceEvents,
        int persistedBuckets,
        Instant materializedAt
) {
}
