package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

public record RecursalMeshReindexResponse(
        String status,
        String indexName,
        boolean indexRecreated,
        int processed,
        int indexed,
        int skipped,
        Integer batchSize,
        Integer maxDocuments,
        String checkpointKey,
        String checkpointStatus,
        String lockOwner,
        boolean resumedFromCheckpoint,
        String lastProcessedRecursoId) {
}
