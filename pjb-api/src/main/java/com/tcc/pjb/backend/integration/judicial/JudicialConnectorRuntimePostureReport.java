package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorRuntimePostureReport(
        Instant generatedAt,
        String tribunalCodigo,
        int totalSystems,
        int healthySystems,
        int degradedSystems,
        int quarantinedSystems,
        int blockedSystems,
        List<JudicialConnectorRuntimePostureSystemReport> systems,
        List<String> alerts,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("totalSystems", totalSystems);
        out.put("healthySystems", healthySystems);
        out.put("degradedSystems", degradedSystems);
        out.put("quarantinedSystems", quarantinedSystems);
        out.put("blockedSystems", blockedSystems);
        out.put("systems", systems == null ? List.of() : systems.stream().map(JudicialConnectorRuntimePostureSystemReport::toMap).toList());
        out.put("alerts", alerts == null ? List.of() : alerts);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
