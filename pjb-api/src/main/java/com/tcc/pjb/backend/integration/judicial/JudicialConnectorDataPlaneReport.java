package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorDataPlaneReport(
        Instant generatedAt,
        String tribunalCodigo,
        Instant horizonStart,
        long totalEvents,
        List<String> readySystems,
        List<JudicialConnectorDataPlaneSystemReport> systems,
        List<String> alerts,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("horizonStart", horizonStart != null ? horizonStart.toString() : null);
        out.put("totalEvents", totalEvents);
        out.put("readySystems", readySystems == null ? List.of() : readySystems);
        out.put("systems", systems == null ? List.of() : systems.stream().map(JudicialConnectorDataPlaneSystemReport::toMap).toList());
        out.put("alerts", alerts == null ? List.of() : alerts);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
