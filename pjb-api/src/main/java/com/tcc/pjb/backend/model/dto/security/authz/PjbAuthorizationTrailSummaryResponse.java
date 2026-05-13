package com.tcc.pjb.backend.model.dto.security.authz;

import java.util.List;

public record PjbAuthorizationTrailSummaryResponse(
        long total,
        long allowed,
        long denied,
        long critico,
        long governanceRequired,
        long governanceDenied,
        long stepUpRequired,
        long stepUpDenied,
        List<PjbAuthorizationTrailBucketResponse> byAction,
        List<PjbAuthorizationTrailBucketResponse> byResourceType,
        List<PjbAuthorizationTrailBucketResponse> byIntegration,
        List<PjbAuthorizationTrailBucketResponse> byInstitutionalCapability
) {
    public PjbAuthorizationTrailSummaryResponse {
        byAction = byAction == null ? List.of() : List.copyOf(byAction);
        byResourceType = byResourceType == null ? List.of() : List.copyOf(byResourceType);
        byIntegration = byIntegration == null ? List.of() : List.copyOf(byIntegration);
        byInstitutionalCapability = byInstitutionalCapability == null ? List.of() : List.copyOf(byInstitutionalCapability);
    }
}
