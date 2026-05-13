package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorObservabilitySystemReport(
        Instant generatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        String observabilityStatus,
        boolean productionReady,
        boolean tribunalReady,
        boolean submissionReady,
        boolean syncReady,
        boolean telemetryPresent,
        long totalEvents,
        double successRate,
        Instant latestEventAt,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("observabilityStatus", observabilityStatus);
        out.put("productionReady", productionReady);
        out.put("tribunalReady", tribunalReady);
        out.put("submissionReady", submissionReady);
        out.put("syncReady", syncReady);
        out.put("telemetryPresent", telemetryPresent);
        out.put("totalEvents", totalEvents);
        out.put("successRate", successRate);
        out.put("latestEventAt", latestEventAt != null ? latestEventAt.toString() : null);
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
