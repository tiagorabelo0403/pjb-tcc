package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailRetentionResponse(
        int totalPersisted,
        Instant oldestOccurredAt,
        Instant newestOccurredAt,
        Instant retentionCutoff,
        long retentionDays,
        int eligibleForPurge,
        int suggestedExportLimit
) {
}
