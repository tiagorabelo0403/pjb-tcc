package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;

public record RecursalMeshIndexDriftReport(
        String status,
        String indexName,
        long projectionCount,
        long indexCount,
        int sampled,
        int missingInIndex,
        int outdatedInIndex,
        int divergentState,
        int divergentRevision,
        String severity,
        Instant checkedAt) {
}
