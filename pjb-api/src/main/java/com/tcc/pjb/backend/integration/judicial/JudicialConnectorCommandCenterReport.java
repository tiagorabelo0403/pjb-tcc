package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorCommandCenterReport(
        Instant generatedAt,
        String tribunalCodigo,
        JudicialConnectorGovernanceReport governance,
        JudicialConnectorControlPlaneReport controlPlane,
        JudicialConnectorDataPlaneReport dataPlane,
        JudicialConnectorCryptographyReport cryptography,
        JudicialConnectorObservabilityReport observability,
        JudicialConnectorPolicyReport policies,
        List<Map<String, Object>> recentOperations,
        List<String> alerts,
        Map<String, Object> metadata
) {
    public JudicialConnectorCommandCenterReport(Instant generatedAt, String tribunalCodigo, JudicialConnectorGovernanceReport governance, JudicialConnectorControlPlaneReport controlPlane, JudicialConnectorDataPlaneReport dataPlane, JudicialConnectorCryptographyReport cryptography, JudicialConnectorObservabilityReport observability, JudicialConnectorPolicyReport policies, List<Map<String, Object>> recentOperations, Map<String, Object> metadata) {
        this(generatedAt, tribunalCodigo, governance, controlPlane, dataPlane, cryptography, observability, policies, recentOperations, List.of(), metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("governance", governance != null ? governance.toMap() : Map.of());
        out.put("controlPlane", controlPlane != null ? controlPlane.toMap() : Map.of());
        out.put("dataPlane", dataPlane != null ? dataPlane.toMap() : Map.of());
        out.put("cryptography", cryptography != null ? cryptography.toMap() : Map.of());
        out.put("observability", observability != null ? observability.toMap() : Map.of());
        out.put("policies", policies != null ? policies.toMap() : Map.of());
        out.put("recentOperations", recentOperations == null ? List.of() : recentOperations);
        out.put("alerts", alerts == null ? List.of() : alerts);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
