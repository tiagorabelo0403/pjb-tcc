package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorContingencyProfile(
        String fallbackMode,
        String contingencyDesk,
        String replayQueue,
        String evidenceRetentionPolicy,
        String manualSubmissionDesk,
        String receiptGuaranteeMode,
        String contingencyWindow,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public JudicialConnectorContingencyProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(fallbackMode, "FALLBACK"),
                firstNonBlank(contingencyDesk, "DESK"),
                firstNonBlank(receiptGuaranteeMode, "ACK"),
                firstNonBlank(contingencyWindow, "WINDOW"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("fallbackMode", fallbackMode);
        out.put("contingencyDesk", contingencyDesk);
        out.put("replayQueue", replayQueue);
        out.put("evidenceRetentionPolicy", evidenceRetentionPolicy);
        out.put("manualSubmissionDesk", manualSubmissionDesk);
        out.put("receiptGuaranteeMode", receiptGuaranteeMode);
        out.put("contingencyWindow", contingencyWindow);
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
