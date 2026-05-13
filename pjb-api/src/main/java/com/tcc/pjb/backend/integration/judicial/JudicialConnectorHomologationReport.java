package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorHomologationReport(
        Instant generatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        boolean productionReady,
        boolean tribunalHomologated,
        boolean tribunalBlocked,
        boolean submitHomologated,
        boolean syncHomologated,
        String effectiveSubmitPath,
        String effectiveDryRunPath,
        String effectiveSnapshotPath,
        String effectiveEventsPath,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("productionReady", productionReady);
        out.put("tribunalHomologated", tribunalHomologated);
        out.put("tribunalBlocked", tribunalBlocked);
        out.put("submitHomologated", submitHomologated);
        out.put("syncHomologated", syncHomologated);
        out.put("effectiveSubmitPath", effectiveSubmitPath);
        out.put("effectiveDryRunPath", effectiveDryRunPath);
        out.put("effectiveSnapshotPath", effectiveSnapshotPath);
        out.put("effectiveEventsPath", effectiveEventsPath);
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
