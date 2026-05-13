package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralDecisionQualityReport(
        Instant generatedAt,
        double convergenceScore,
        double evidenceScore,
        double reviewPressureScore,
        double determinismScore,
        boolean safeAutomationEligible,
        String operatingModeHint,
        List<String> axisConsensus,
        List<String> conflicts,
        List<String> strongSignals,
        List<String> weakSignals,
        List<String> riskSignals,
        Map<String, Object> metadata
) {

    public ProceduralDecisionQualityReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        axisConsensus = axisConsensus == null ? List.of() : List.copyOf(axisConsensus);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        strongSignals = strongSignals == null ? List.of() : List.copyOf(strongSignals);
        weakSignals = weakSignals == null ? List.of() : List.copyOf(weakSignals);
        riskSignals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("convergenceScore", convergenceScore);
        out.put("evidenceScore", evidenceScore);
        out.put("reviewPressureScore", reviewPressureScore);
        out.put("determinismScore", determinismScore);
        out.put("safeAutomationEligible", safeAutomationEligible);
        out.put("operatingModeHint", operatingModeHint);
        out.put("axisConsensus", axisConsensus);
        out.put("conflicts", conflicts);
        out.put("strongSignals", strongSignals);
        out.put("weakSignals", weakSignals);
        out.put("riskSignals", riskSignals);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
