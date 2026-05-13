package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorCryptographyReport(
        Instant generatedAt,
        String tribunalCodigo,
        int strongAuthenticationCount,
        int certificateReadyCount,
        int blockedCount,
        List<JudicialConnectorCryptographySystemReport> systems,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("strongAuthenticationCount", strongAuthenticationCount);
        out.put("certificateReadyCount", certificateReadyCount);
        out.put("blockedCount", blockedCount);
        out.put("systems", systems == null ? List.of() : systems.stream().map(JudicialConnectorCryptographySystemReport::toMap).toList());
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
