package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailForensicsSummaryResponse(
        long total,
        long allowed,
        long denied,
        long critico,
        long governanceDenied,
        long stepUpDenied,
        Instant oldestOccurredAt,
        Instant newestOccurredAt
) {
}
