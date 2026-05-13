package com.tcc.pjb.backend.platform.runtime.domain;

import java.time.Instant;
import java.util.List;

public record PjbRuntimePressureView(
        boolean ready,
        boolean warmingUp,
        int pressureScore,
        int headroomScore,
        String componentRole,
        int availableProcessors,
        long maxMemoryMiB,
        Instant sampledAt,
        long uptimeMillis,
        List<String> overloadedExecutors,
        List<String> degradedDatasources,
        List<String> criticalOverloadedExecutors,
        boolean criticalMemoryRunaway,
        boolean criticalGcPressure,
        String trend,
        int alertCount
) {
}
