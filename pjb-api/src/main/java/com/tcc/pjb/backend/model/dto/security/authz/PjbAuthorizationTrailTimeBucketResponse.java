package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailTimeBucketResponse(
        Instant startedAt,
        Instant endedAtExclusive,
        String label,
        long total,
        long allowed,
        long denied,
        long critico,
        long governanceDenied,
        long stepUpDenied
) {
}
