package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorCryptoCommandCenterReport(
        Instant generatedAt,
        String tribunalCodigo,
        JudicialConnectorCryptoPostureSummary postureSummary,
        JudicialConnectorSecurityPackSummary packSummary,
        List<JudicialConnectorSecurityPackReport> effectivePacks,
        List<JudicialConnectorCertificateInventoryReport> inventory,
        JudicialConnectorSecuritySessionSummary sessionSummary,
        List<JudicialConnectorSecuritySessionReport> recentSessions,
        List<Map<String, Object>> recentFailures,
        List<Map<String, Object>> recentAdminOperations,
        List<String> alerts,
        Map<String, Object> metadata
) {
    public JudicialConnectorCryptoCommandCenterReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        effectivePacks = effectivePacks == null ? List.of() : List.copyOf(effectivePacks);
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
        recentSessions = recentSessions == null ? List.of() : List.copyOf(recentSessions);
        recentFailures = recentFailures == null ? List.of() : List.copyOf(recentFailures);
        recentAdminOperations = recentAdminOperations == null ? List.of() : List.copyOf(recentAdminOperations);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("postureSummary", postureSummary != null ? postureSummary.toMap() : null);
        out.put("packSummary", packSummary != null ? packSummary.toMap() : null);
        out.put("effectivePacks", effectivePacks.stream().map(JudicialConnectorSecurityPackReport::toMap).toList());
        out.put("inventory", inventory.stream().map(JudicialConnectorCertificateInventoryReport::toMap).toList());
        out.put("sessionSummary", sessionSummary != null ? sessionSummary.toMap() : null);
        out.put("recentSessions", recentSessions.stream().map(JudicialConnectorSecuritySessionReport::toMap).toList());
        out.put("recentFailures", recentFailures);
        out.put("recentAdminOperations", recentAdminOperations);
        out.put("alerts", alerts);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
