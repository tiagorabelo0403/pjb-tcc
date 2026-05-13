package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleMetricsView(
        Long bundleId,
        int actionsCount,
        int conflictsCount,
        Instant updatedAt
) {}
