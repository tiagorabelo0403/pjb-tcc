package com.tcc.pjb.backend.platform.runtime.domain;

import java.time.Instant;

public record PjbRuntimeDrainView(
        boolean draining,
        boolean readyForTraffic,
        Instant drainingSince,
        long drainAgeMillis,
        String reason,
        long quietPeriodMillis
) {
}
