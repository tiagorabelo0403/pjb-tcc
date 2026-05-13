package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorDataPlaneSystemReport(
        Instant generatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        String executionStatus,
        boolean submissionReady,
        boolean syncReady,
        long totalEvents,
        long acceptedSubmissions,
        long rejectedSubmissions,
        long snapshotHits,
        long eventSyncHits,
        double successRate,
        Instant latestEventAt,
        JudicialConnectorOperationalProfileReport operationalProfile,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("executionStatus", executionStatus);
        out.put("submissionReady", submissionReady);
        out.put("syncReady", syncReady);
        out.put("totalEvents", totalEvents);
        out.put("acceptedSubmissions", acceptedSubmissions);
        out.put("rejectedSubmissions", rejectedSubmissions);
        out.put("snapshotHits", snapshotHits);
        out.put("eventSyncHits", eventSyncHits);
        out.put("successRate", successRate);
        out.put("latestEventAt", latestEventAt != null ? latestEventAt.toString() : null);
        out.put("operationalProfile", operationalProfile != null ? operationalProfile.toMap() : Map.of());
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
