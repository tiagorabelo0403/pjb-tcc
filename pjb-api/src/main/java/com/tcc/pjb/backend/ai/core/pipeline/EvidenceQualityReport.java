package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.List;
import java.util.Map;

public record EvidenceQualityReport(
        int evidenceCount,
        int sourceDiversity,
        double meanScore,
        double sufficiencyScore,
        boolean conflictRisk,
        List<String> missingDataHints,
        Map<String, Object> meta
) {
    public EvidenceQualityReport {
        missingDataHints = (missingDataHints == null) ? List.of() : List.copyOf(missingDataHints);
        meta = (meta == null) ? Map.of() : Map.copyOf(meta);
    }
}
