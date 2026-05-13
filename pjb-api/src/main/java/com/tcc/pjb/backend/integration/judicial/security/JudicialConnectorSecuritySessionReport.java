package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record JudicialConnectorSecuritySessionReport(
        Instant createdAt,
        JudicialSystem system,
        String tribunalCodigo,
        String environmentName,
        String operationName,
        String targetScheme,
        String targetHostSha256,
        Integer targetPort,
        String tlsMode,
        String outcomeStatus,
        boolean success,
        Integer httpStatusCode,
        long durationMillis,
        boolean hardwareBacked,
        boolean mutualTls,
        boolean hostnameVerification,
        String keyStoreRef,
        String trustStoreRef,
        String keyAlias,
        String correlationId,
        Map<String, Object> metadata
) {
    public JudicialConnectorSecuritySessionReport {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("createdAt", createdAt != null ? createdAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("environmentName", environmentName);
        out.put("operationName", operationName);
        out.put("targetScheme", targetScheme);
        out.put("targetHostSha256", targetHostSha256);
        out.put("targetPort", targetPort);
        out.put("tlsMode", tlsMode);
        out.put("outcomeStatus", outcomeStatus);
        out.put("success", success);
        out.put("httpStatusCode", httpStatusCode);
        out.put("durationMillis", durationMillis);
        out.put("hardwareBacked", hardwareBacked);
        out.put("mutualTls", mutualTls);
        out.put("hostnameVerification", hostnameVerification);
        out.put("keyStoreRef", keyStoreRef);
        out.put("trustStoreRef", trustStoreRef);
        out.put("keyAlias", keyAlias);
        out.put("correlationId", correlationId);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
