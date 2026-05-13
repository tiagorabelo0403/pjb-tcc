package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorCryptographySystemReport(
        Instant generatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        String cryptographyStatus,
        JudicialConnectorAuthMode authMode,
        boolean connectorEnabled,
        boolean productionReady,
        boolean tribunalReady,
        boolean certificateRequired,
        boolean certificateConfigured,
        boolean certificateSatisfied,
        boolean authenticationSatisfied,
        boolean strongAuthentication,
        String certificateAlias,
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
        out.put("cryptographyStatus", cryptographyStatus);
        out.put("authMode", authMode != null ? authMode.name() : JudicialConnectorAuthMode.NONE.name());
        out.put("connectorEnabled", connectorEnabled);
        out.put("productionReady", productionReady);
        out.put("tribunalReady", tribunalReady);
        out.put("certificateRequired", certificateRequired);
        out.put("certificateConfigured", certificateConfigured);
        out.put("certificateSatisfied", certificateSatisfied);
        out.put("authenticationSatisfied", authenticationSatisfied);
        out.put("strongAuthentication", strongAuthentication);
        out.put("certificateAlias", certificateAlias);
        out.put("operationalProfile", operationalProfile != null ? operationalProfile.toMap() : Map.of());
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
