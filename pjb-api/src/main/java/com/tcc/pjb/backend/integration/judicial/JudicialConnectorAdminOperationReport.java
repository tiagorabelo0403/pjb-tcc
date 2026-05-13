package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorAdminOperationReport(
        Instant generatedAt,
        String operationType,
        JudicialSystem system,
        String tribunalCodigo,
        String environmentName,
        String outcomeStatus,
        String outcomeMessage,
        JudicialConnectorPolicyOverlay policy,
        JudicialConnectorControlPlaneReport controlPlane,
        JudicialConnectorDataPlaneReport dataPlane,
        List<Map<String, Object>> recentOperations,
        Map<String, Object> metadata
) {
    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("operationType", operationType);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("environmentName", environmentName);
        out.put("outcomeStatus", outcomeStatus);
        out.put("outcomeMessage", outcomeMessage);
        out.put("policy", policy != null ? policy.toMap() : Map.of());
        out.put("controlPlane", controlPlane != null ? controlPlane.toMap() : Map.of());
        out.put("dataPlane", dataPlane != null ? dataPlane.toMap() : Map.of());
        out.put("recentOperations", recentOperations == null ? List.of() : recentOperations);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
