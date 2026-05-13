package com.tcc.pjb.backend.model.dto.security.operational;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OperationalCredentialSnapshotResponse(
        String laneCode,
        List<Entry> entries,
        Map<String, Object> directorGovernance,
        Map<String, Object> routes
) {
    public record Entry(
            String functionCode,
            String label,
            String status,
            boolean provisionedByInstitution,
            boolean active,
            boolean resetRequired,
            boolean locked,
            String justicaAxis,
            String tribunalCodigo,
            String forumCode,
            String unitCode,
            String varaLabel,
            String uf,
            String comarca,
            LocalDateTime activatedAt,
            LocalDateTime lastVerifiedAt,
            LocalDateTime lastResetAt,
            Map<String, Object> policy,
            Map<String, Object> routes
    ) {
    }
}
