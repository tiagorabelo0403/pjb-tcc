package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorSecurityPackSummary(
        Instant generatedAt,
        int totalPacks,
        int mutualTlsPacks,
        int hostnameVerifiedPacks,
        int revocationEnforcedPacks,
        int tribunalScopedPacks,
        int hardwareBoundKeyStoreReferences,
        List<Map<String, Object>> targets,
        Map<String, Object> metadata
) {
    public JudicialConnectorSecurityPackSummary {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        targets = targets == null ? List.of() : List.copyOf(targets);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("totalPacks", totalPacks);
        out.put("mutualTlsPacks", mutualTlsPacks);
        out.put("hostnameVerifiedPacks", hostnameVerifiedPacks);
        out.put("revocationEnforcedPacks", revocationEnforcedPacks);
        out.put("tribunalScopedPacks", tribunalScopedPacks);
        out.put("hardwareBoundKeyStoreReferences", hardwareBoundKeyStoreReferences);
        out.put("targets", targets);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
