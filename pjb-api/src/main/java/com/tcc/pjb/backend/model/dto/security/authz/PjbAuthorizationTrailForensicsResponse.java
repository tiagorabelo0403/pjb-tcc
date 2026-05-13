package com.tcc.pjb.backend.model.dto.security.authz;

import java.util.List;

public record PjbAuthorizationTrailForensicsResponse(
        String sourceMode,
        String granularity,
        int sampled,
        int limitApplied,
        PjbAuthorizationTrailForensicsSummaryResponse summary,
        List<PjbAuthorizationTrailTimeBucketResponse> timeSeries,
        List<PjbAuthorizationTrailBucketResponse> byIntegration,
        List<PjbAuthorizationTrailBucketResponse> byInstitutionalUnit,
        List<PjbAuthorizationTrailBucketResponse> byResourceType,
        List<PjbAuthorizationTrailBucketResponse> byGovernanceScope
) {
    public PjbAuthorizationTrailForensicsResponse {
        sourceMode = sourceMode == null || sourceMode.isBlank() ? "PERSISTED" : sourceMode;
        granularity = granularity == null || granularity.isBlank() ? "DAY" : granularity;
        timeSeries = timeSeries == null ? List.of() : List.copyOf(timeSeries);
        byIntegration = byIntegration == null ? List.of() : List.copyOf(byIntegration);
        byInstitutionalUnit = byInstitutionalUnit == null ? List.of() : List.copyOf(byInstitutionalUnit);
        byResourceType = byResourceType == null ? List.of() : List.copyOf(byResourceType);
        byGovernanceScope = byGovernanceScope == null ? List.of() : List.copyOf(byGovernanceScope);
    }
}
