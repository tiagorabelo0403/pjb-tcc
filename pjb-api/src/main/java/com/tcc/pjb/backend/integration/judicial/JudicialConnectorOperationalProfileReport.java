package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorOperationalProfileReport(
        Instant generatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        boolean connectorRegistered,
        boolean connectorEnabled,
        boolean connectorOperational,
        boolean readyForProduction,
        boolean readyForTribunalSubmission,
        JudicialConnectorAuthMode authMode,
        JudicialConnectorHomologationReport homologation,
        JudicialConnectorReadinessReport readiness,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("connectorRegistered", connectorRegistered);
        out.put("connectorEnabled", connectorEnabled);
        out.put("connectorOperational", connectorOperational);
        out.put("readyForProduction", readyForProduction);
        out.put("readyForTribunalSubmission", readyForTribunalSubmission);
        out.put("authMode", authMode != null ? authMode.name() : JudicialConnectorAuthMode.NONE.name());
        out.put("homologation", homologation != null ? homologation.toMap() : Map.of());
        out.put("readiness", readiness != null ? readiness.toMap() : Map.of());
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
