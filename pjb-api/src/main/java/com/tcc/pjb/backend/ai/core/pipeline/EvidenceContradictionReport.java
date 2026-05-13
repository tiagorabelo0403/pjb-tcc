package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.List;
import java.util.Map;

public record EvidenceContradictionReport(
        double contradictionScore,
        double inconsistencyScore,
        int positiveStance,
        int negativeStance,
        int uncertainStance,
        int unknownStance,
        int temporalSpreadYears,
        boolean mixedJurisdiction,
        List<String> signals,
        Map<String, Object> meta
) {
    public EvidenceContradictionReport {
        signals = (signals == null) ? List.of() : List.copyOf(signals);
        meta = (meta == null) ? Map.of() : Map.copyOf(meta);
    }
}
