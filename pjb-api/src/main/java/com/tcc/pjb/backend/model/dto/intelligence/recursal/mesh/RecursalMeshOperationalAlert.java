package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;

public record RecursalMeshOperationalAlert(
        String code,
        String severity,
        String title,
        String detail,
        Instant generatedAt) {
}
