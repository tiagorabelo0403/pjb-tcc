package com.tcc.pjb.backend.model.dto.julgamento.coverage;

import java.time.Instant;
import java.util.List;

public record JulgamentoCoverageAuditView(
        Long id,
        Long processoId,
        Long usuarioId,
        String actType,
        String overallStatus,
        Integer overallScore,
        String recursalSpecies,
        List<String> highlights,
        Instant createdAt
) {
}
