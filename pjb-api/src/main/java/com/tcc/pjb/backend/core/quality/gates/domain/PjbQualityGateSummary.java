package com.tcc.pjb.backend.core.quality.gates.domain;

import java.time.Instant;

public record PjbQualityGateSummary(
        boolean buildApproved,
        boolean qualityMatrixApproved,
        boolean architectureReady,
        boolean contractReady,
        boolean mutationReady,
        boolean dastReady,
        boolean integrationReady,
        int blockerCount,
        Instant generatedAt
) {
}
