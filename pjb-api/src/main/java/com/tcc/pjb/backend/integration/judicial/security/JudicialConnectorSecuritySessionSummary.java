package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorSecuritySessionSummary(
        Instant generatedAt,
        String tribunalCodigo,
        long sessionCount,
        long successCount,
        long remoteFailureCount,
        long transportFailureCount,
        long mutualTlsCount,
        long hardwareBackedCount,
        long hostnameVerifiedCount,
        long averageDurationMillis,
        long maxDurationMillis,
        Instant latestSessionAt,
        List<Map<String, Object>> outcomeBreakdown,
        Map<String, Object> metadata
) {
    public JudicialConnectorSecuritySessionSummary {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        outcomeBreakdown = outcomeBreakdown == null ? List.of() : List.copyOf(outcomeBreakdown);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("sessionCount", sessionCount);
        out.put("successCount", successCount);
        out.put("remoteFailureCount", remoteFailureCount);
        out.put("transportFailureCount", transportFailureCount);
        out.put("mutualTlsCount", mutualTlsCount);
        out.put("hardwareBackedCount", hardwareBackedCount);
        out.put("hostnameVerifiedCount", hostnameVerifiedCount);
        out.put("averageDurationMillis", averageDurationMillis);
        out.put("maxDurationMillis", maxDurationMillis);
        out.put("latestSessionAt", latestSessionAt != null ? latestSessionAt.toString() : null);
        out.put("outcomeBreakdown", outcomeBreakdown);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
