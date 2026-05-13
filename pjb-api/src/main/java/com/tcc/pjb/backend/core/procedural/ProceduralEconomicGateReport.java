package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralEconomicGateReport(
        Instant generatedAt,
        String thresholdKind,
        String thresholdCode,
        String economicBand,
        boolean routeCompatible,
        boolean blocking,
        boolean alert,
        String currentTrack,
        String recommendedCompetence,
        String recommendedRito,
        Integer salaryReferenceYear,
        String operationalSuggestion,
        List<String> reasons,
        List<String> rerouteOptions,
        List<String> reviewChecklist,
        Map<String, Object> metrics
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("thresholdKind", thresholdKind);
        out.put("thresholdCode", thresholdCode);
        out.put("economicBand", economicBand);
        out.put("routeCompatible", routeCompatible);
        out.put("blocking", blocking);
        out.put("alert", alert);
        out.put("currentTrack", currentTrack);
        out.put("recommendedCompetence", recommendedCompetence);
        out.put("recommendedRito", recommendedRito);
        out.put("salaryReferenceYear", salaryReferenceYear);
        out.put("operationalSuggestion", operationalSuggestion);
        out.put("reasons", reasons == null ? List.of() : reasons);
        out.put("rerouteOptions", rerouteOptions == null ? List.of() : rerouteOptions);
        out.put("reviewChecklist", reviewChecklist == null ? List.of() : reviewChecklist);
        LinkedHashMap<String, Object> safeMetrics = metrics == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metrics);
        safeMetrics.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        out.put("metrics", safeMetrics);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
