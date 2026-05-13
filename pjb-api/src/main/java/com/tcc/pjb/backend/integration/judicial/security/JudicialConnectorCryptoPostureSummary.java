package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorCryptoPostureSummary(
        Instant generatedAt,
        int total,
        int valid,
        int warning,
        int blocked,
        int expired,
        int expiringSoon,
        int hardwareBacked,
        int withRecentFailures,
        List<Map<String, Object>> blockedTargets,
        List<Map<String, Object>> expiringTargets,
        Map<String, Object> metadata
) {
    public JudicialConnectorCryptoPostureSummary {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockedTargets = blockedTargets == null ? List.of() : List.copyOf(blockedTargets);
        expiringTargets = expiringTargets == null ? List.of() : List.copyOf(expiringTargets);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }



    public int blockedCount() {
        return blocked;
    }

    public int expiredCount() {
        return expired;
    }

    public int expiringSoonCount() {
        return expiringSoon;
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt.toString());
        out.put("total", total);
        out.put("valid", valid);
        out.put("warning", warning);
        out.put("blocked", blocked);
        out.put("expired", expired);
        out.put("expiringSoon", expiringSoon);
        out.put("hardwareBacked", hardwareBacked);
        out.put("withRecentFailures", withRecentFailures);
        out.put("blockedTargets", blockedTargets);
        out.put("expiringTargets", expiringTargets);
        out.put("metadata", metadata);
        return Map.copyOf(out);
    }
}
