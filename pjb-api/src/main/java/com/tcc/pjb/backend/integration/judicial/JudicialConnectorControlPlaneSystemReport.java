package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorControlPlaneSystemReport(
        Instant generatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        String controlStatus,
        boolean connectorRegistered,
        boolean connectorEnabled,
        boolean connectorOperational,
        boolean productionReady,
        boolean tribunalReady,
        JudicialConnectorAuthMode authMode,
        JudicialConnectorGovernanceItem governance,
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
        out.put("controlStatus", controlStatus);
        out.put("connectorRegistered", connectorRegistered);
        out.put("connectorEnabled", connectorEnabled);
        out.put("connectorOperational", connectorOperational);
        out.put("productionReady", productionReady);
        out.put("tribunalReady", tribunalReady);
        out.put("authMode", authMode != null ? authMode.name() : JudicialConnectorAuthMode.NONE.name());
        out.put("governance", governance != null ? governance.toMap() : Map.of());
        out.put("operationalProfile", operationalProfile != null ? operationalProfile.toMap() : Map.of());
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
