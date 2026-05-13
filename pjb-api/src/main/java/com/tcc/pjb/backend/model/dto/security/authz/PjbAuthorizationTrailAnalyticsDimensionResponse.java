package com.tcc.pjb.backend.model.dto.security.authz;

import java.util.List;

public record PjbAuthorizationTrailAnalyticsDimensionResponse(
        String dimensionType,
        List<PjbAuthorizationTrailBucketResponse> buckets
) {
    public PjbAuthorizationTrailAnalyticsDimensionResponse {
        dimensionType = dimensionType == null || dimensionType.isBlank() ? "OVERVIEW" : dimensionType;
        buckets = buckets == null ? List.of() : List.copyOf(buckets);
    }
}
