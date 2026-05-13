package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;
import java.util.List;

public record PjbAuthorizationTrailAnalyticsOperationalStatusResponse(
        boolean incrementalRefreshEnabled,
        int readModelTotalEntries,
        Instant readModelOldestOccurredAt,
        Instant readModelNewestOccurredAt,
        List<PjbAuthorizationTrailAnalyticsGranularityStatusResponse> granularities,
        PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse refreshQueue
) {
    public PjbAuthorizationTrailAnalyticsOperationalStatusResponse {
        granularities = granularities == null ? List.of() : List.copyOf(granularities);
    }
}
