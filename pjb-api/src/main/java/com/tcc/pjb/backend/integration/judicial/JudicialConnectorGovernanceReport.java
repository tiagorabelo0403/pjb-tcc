package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorGovernanceReport(
        Instant generatedAt,
        int registeredConnectorCount,
        int enabledConnectorCount,
        int operationalConnectorCount,
        boolean contingencyConnectorPresent,
        List<JudicialConnectorGovernanceItem> connectors,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("registeredConnectorCount", registeredConnectorCount);
        out.put("enabledConnectorCount", enabledConnectorCount);
        out.put("operationalConnectorCount", operationalConnectorCount);
        out.put("contingencyConnectorPresent", contingencyConnectorPresent);
        out.put("connectors", connectors == null ? List.of() : connectors.stream().map(JudicialConnectorGovernanceItem::toMap).toList());
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
