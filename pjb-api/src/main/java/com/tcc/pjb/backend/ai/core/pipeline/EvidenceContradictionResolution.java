package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EvidenceContradictionResolution(
        boolean resolved,
        double residualInconsistencyScore,
        String rationale,
        List<String> requiredClarifications,
        Map<String, Object> meta
) {
    public EvidenceContradictionResolution {
        requiredClarifications = (requiredClarifications == null) ? List.of() : List.copyOf(requiredClarifications);
        meta = (meta == null) ? Map.of() : Map.copyOf(meta);
    }

    public static EvidenceContradictionResolution passthrough(EvidenceContradictionReport report) {
        double s = (report == null) ? 0.0 : report.inconsistencyScore();
        return new EvidenceContradictionResolution(true, s, "below_threshold", List.of(), Map.of());
    }

    public static EvidenceContradictionResolution deny(String rationale, double residual, List<String> clarifications, Map<String, Object> meta) {
        Map<String, Object> m = (meta == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(meta);
        if (rationale != null) m.putIfAbsent("rationale", rationale);
        return new EvidenceContradictionResolution(false, Math.max(0.0, Math.min(1.0, residual)), rationale, clarifications, m);
    }

    public static EvidenceContradictionResolution allow(String rationale, double residual, Map<String, Object> meta) {
        Map<String, Object> m = (meta == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(meta);
        if (rationale != null) m.putIfAbsent("rationale", rationale);
        return new EvidenceContradictionResolution(true, Math.max(0.0, Math.min(1.0, residual)), rationale, List.of(), m);
    }
}
