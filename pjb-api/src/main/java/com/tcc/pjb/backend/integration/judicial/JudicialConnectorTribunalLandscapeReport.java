package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorTribunalLandscapeReport(
        Instant generatedAt,
        String tribunalCodigo,
        List<String> readySystems,
        List<String> productionReadySystems,
        List<JudicialConnectorOperationalProfileReport> profiles,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("readySystems", readySystems == null ? List.of() : readySystems);
        out.put("productionReadySystems", productionReadySystems == null ? List.of() : productionReadySystems);
        out.put("profiles", profiles == null ? List.of() : profiles.stream().map(JudicialConnectorOperationalProfileReport::toMap).toList());
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
