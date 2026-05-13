package com.tcc.pjb.backend.core.kernel.advisory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record KernelAdvisoryTelemetry(
        String scope,
        String snapshotId,
        String statusBand,
        Instant generatedAt,
        String ritoName,
        int advisoryCount,
        int blockingCount,
        List<String> components,
        Map<String, Object> diagnostics
) {
}
