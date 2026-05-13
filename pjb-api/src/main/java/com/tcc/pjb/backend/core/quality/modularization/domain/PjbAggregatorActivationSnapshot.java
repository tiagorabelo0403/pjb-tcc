package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.time.Instant;
import java.util.List;

public record PjbAggregatorActivationSnapshot(
        boolean phaseOneAggregatorFilePresent,
        boolean rootPomAlreadyLinked,
        boolean activationReady,
        int moduleCount,
        int checklistSatisfiedCount,
        List<String> blockers,
        Instant generatedAt) {
}
