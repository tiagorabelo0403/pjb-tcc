package com.tcc.pjb.backend.model.dto.security.authz;

public record PjbAuthorizationTrailBucketResponse(
        String code,
        long total,
        long allowed,
        long denied
) {
}
