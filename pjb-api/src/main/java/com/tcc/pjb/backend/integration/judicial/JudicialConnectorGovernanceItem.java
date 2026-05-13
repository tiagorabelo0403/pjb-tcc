package com.tcc.pjb.backend.integration.judicial;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorGovernanceItem(
        JudicialSystem system,
        boolean connectorRegistered,
        boolean configuredEnabled,
        boolean operational,
        boolean productionReady,
        JudicialConnectorAuthMode authMode,
        List<String> homologatedTribunals,
        List<String> blockedTribunals,
        List<String> conflictingTribunals,
        JudicialConnectorOperationalProfileReport defaultProfile,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("system", system != null ? system.name() : null);
        out.put("connectorRegistered", connectorRegistered);
        out.put("configuredEnabled", configuredEnabled);
        out.put("operational", operational);
        out.put("productionReady", productionReady);
        out.put("authMode", authMode != null ? authMode.name() : JudicialConnectorAuthMode.NONE.name());
        out.put("homologatedTribunals", homologatedTribunals == null ? List.of() : homologatedTribunals);
        out.put("blockedTribunals", blockedTribunals == null ? List.of() : blockedTribunals);
        out.put("conflictingTribunals", conflictingTribunals == null ? List.of() : conflictingTribunals);
        out.put("defaultProfile", defaultProfile != null ? defaultProfile.toMap() : Map.of());
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
