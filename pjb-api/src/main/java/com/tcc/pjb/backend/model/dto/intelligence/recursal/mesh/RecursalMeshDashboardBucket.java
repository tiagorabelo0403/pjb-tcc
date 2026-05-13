package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

public record RecursalMeshDashboardBucket(
        String key,
        long total) {
    public long value() {
        return total;
    }
}
