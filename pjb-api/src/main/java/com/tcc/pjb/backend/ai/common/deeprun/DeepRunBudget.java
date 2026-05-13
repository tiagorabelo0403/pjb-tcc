package com.tcc.pjb.backend.ai.common.deeprun;

import java.time.Duration;

public record DeepRunBudget(
        Duration maxDuration,
        int maxSteps,
        int maxArtifacts
) {
    public static DeepRunBudget default48h() {
        return new DeepRunBudget(Duration.ofHours(48), 10_000, 10_000);
    }
}
