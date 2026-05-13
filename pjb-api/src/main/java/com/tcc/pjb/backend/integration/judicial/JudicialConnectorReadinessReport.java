package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorReadinessReport(
        Instant generatedAt,
        JudicialSystem system,
        boolean connectorEnabled,
        boolean baseUrlConfigured,
        boolean protocolPathResolved,
        boolean syncPathResolved,
        boolean authenticationSatisfied,
        boolean certificateSatisfied,
        boolean readyForDryRun,
        boolean readyForSubmission,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("connectorEnabled", connectorEnabled);
        out.put("baseUrlConfigured", baseUrlConfigured);
        out.put("protocolPathResolved", protocolPathResolved);
        out.put("syncPathResolved", syncPathResolved);
        out.put("authenticationSatisfied", authenticationSatisfied);
        out.put("certificateSatisfied", certificateSatisfied);
        out.put("readyForDryRun", readyForDryRun);
        out.put("readyForSubmission", readyForSubmission);
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
