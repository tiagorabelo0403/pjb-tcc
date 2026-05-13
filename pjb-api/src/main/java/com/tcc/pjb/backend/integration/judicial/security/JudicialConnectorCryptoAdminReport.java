package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorCryptoAdminReport(
        Instant generatedAt,
        String operationType,
        JudicialSystem system,
        String tribunalCodigo,
        String targetUrl,
        String outcomeStatus,
        String outcomeMessage,
        JudicialCertificateValidationReport certificateValidation,
        Integer httpStatus,
        Duration duration,
        List<Map<String, Object>> recentOperations,
        Map<String, Object> metadata
) {
    public JudicialConnectorCryptoAdminReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        recentOperations = recentOperations == null ? List.of() : List.copyOf(recentOperations);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("operationType", operationType);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("targetUrl", targetUrl);
        out.put("outcomeStatus", outcomeStatus);
        out.put("outcomeMessage", outcomeMessage);
        out.put("certificateValidation", certificateValidation != null ? certificateValidation.toMap() : Map.of());
        out.put("httpStatus", httpStatus);
        out.put("durationMs", duration != null ? duration.toMillis() : null);
        out.put("recentOperations", recentOperations);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
