package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorRuntimePostureSystemReport(
        Instant generatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        String runtimeStatus,
        boolean quarantineActive,
        boolean maintenanceMode,
        boolean readOnlyProjectionRecommended,
        boolean backpressureRecommended,
        boolean resyncRecommended,
        Instant latestEventAt,
        long stalenessSeconds,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("runtimeStatus", runtimeStatus);
        out.put("quarantineActive", quarantineActive);
        out.put("maintenanceMode", maintenanceMode);
        out.put("readOnlyProjectionRecommended", readOnlyProjectionRecommended);
        out.put("backpressureRecommended", backpressureRecommended);
        out.put("resyncRecommended", resyncRecommended);
        out.put("latestEventAt", latestEventAt != null ? latestEventAt.toString() : null);
        out.put("stalenessSeconds", stalenessSeconds);
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
