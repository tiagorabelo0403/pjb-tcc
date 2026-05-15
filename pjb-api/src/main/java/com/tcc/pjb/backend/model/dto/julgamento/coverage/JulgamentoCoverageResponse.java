package com.tcc.pjb.backend.model.dto.julgamento.coverage;

import java.util.List;
import java.util.Map;

public record JulgamentoCoverageResponse(
        Long processoId,
        String numeroProcesso,
        String overallStatus,
        int overallScore,
        List<String> highlights,
        JulgamentoCoveragePaneResponse envelope,
        JulgamentoCoveragePaneResponse competence,
        JulgamentoCoveragePaneResponse materiality,
        JulgamentoCoveragePaneResponse recursal,
        JulgamentoCoveragePaneResponse publication,
        Map<String, Object> metadata
) {
}
