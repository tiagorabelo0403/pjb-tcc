package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RecursalMeshReindexRequest(
        Boolean recreateIndex,
        @Min(10) @Max(500) Integer batchSize,
        @Min(1) @Max(100000) Integer maxDocuments,
        Boolean refreshAtEnd,
        Boolean resumeFromCheckpoint,
        @Size(max = 120) String checkpointKey) {

    public RecursalMeshReindexRequest(Boolean recreateIndex,
                                      Integer batchSize,
                                      Integer maxDocuments,
                                      Boolean refreshAtEnd) {
        this(recreateIndex, batchSize, maxDocuments, refreshAtEnd, Boolean.TRUE, null);
    }

    public RecursalMeshReindexRequest {
        batchSize = batchSize == null ? 200 : Math.max(10, Math.min(500, batchSize));
        refreshAtEnd = refreshAtEnd == null ? Boolean.TRUE : refreshAtEnd;
        recreateIndex = recreateIndex == null ? Boolean.FALSE : recreateIndex;
        resumeFromCheckpoint = resumeFromCheckpoint == null ? Boolean.TRUE : resumeFromCheckpoint;
        checkpointKey = checkpointKey == null || checkpointKey.isBlank() ? "default" : checkpointKey.trim();
    }
}
