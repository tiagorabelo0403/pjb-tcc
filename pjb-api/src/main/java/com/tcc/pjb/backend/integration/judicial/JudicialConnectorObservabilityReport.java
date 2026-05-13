package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorObservabilityReport(
        Instant generatedAt,
        String tribunalCodigo,
        Instant horizonStart,
        int healthySystems,
        int degradedSystems,
        int blockedSystems,
        List<JudicialConnectorObservabilitySystemReport> systems,
        List<String> alerts,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("horizonStart", horizonStart != null ? horizonStart.toString() : null);
        out.put("healthySystems", healthySystems);
        out.put("degradedSystems", degradedSystems);
        out.put("blockedSystems", blockedSystems);
        out.put("systems", systems == null ? List.of() : systems.stream().map(JudicialConnectorObservabilitySystemReport::toMap).toList());
        out.put("alerts", alerts == null ? List.of() : alerts);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
