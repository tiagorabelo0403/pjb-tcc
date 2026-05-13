package com.tcc.pjb.backend.core.kernel.twin;

import java.util.List;

public record PjbCourtDigitalTwinProjection(
        String status,
        int projectedWeeklyCapacity,
        int projectedBacklogAfterFourWeeks,
        boolean interventionRequired,
        List<String> recommendations
) {
    public PjbCourtDigitalTwinProjection {
        status = status == null || status.isBlank() ? "STABLE" : status.trim();
        projectedWeeklyCapacity = Math.max(0, projectedWeeklyCapacity);
        projectedBacklogAfterFourWeeks = Math.max(0, projectedBacklogAfterFourWeeks);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
