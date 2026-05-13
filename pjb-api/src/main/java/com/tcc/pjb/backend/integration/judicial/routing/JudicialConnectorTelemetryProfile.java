package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorTelemetryProfile(
        String telemetryMode,
        String telemetryChannel,
        String deadLetterQueue,
        String reconciliationDesk,
        String observabilityPolicy,
        String correlationKeyMode,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public JudicialConnectorTelemetryProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(telemetryMode, "TELEMETRY"),
                firstNonBlank(telemetryChannel, "CHANNEL"),
                firstNonBlank(correlationKeyMode, "CORRELATION"),
                firstNonBlank(observabilityPolicy, "POLICY"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("telemetryMode", telemetryMode);
        out.put("telemetryChannel", telemetryChannel);
        out.put("deadLetterQueue", deadLetterQueue);
        out.put("reconciliationDesk", reconciliationDesk);
        out.put("observabilityPolicy", observabilityPolicy);
        out.put("correlationKeyMode", correlationKeyMode);
        out.put("labels", labels);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
