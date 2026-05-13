package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorReplayProfile(
        String replayMode,
        String replayWindow,
        String replayEvidenceDesk,
        String receiptCorrelationMode,
        String receiptDeadlineMode,
        String deliveryAssuranceMode,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public JudicialConnectorReplayProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(replayMode, "REPLAY"),
                firstNonBlank(receiptCorrelationMode, "CORRELATION"),
                firstNonBlank(deliveryAssuranceMode, "ASSURANCE"),
                firstNonBlank(replayWindow, "WINDOW"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("replayMode", replayMode);
        out.put("replayWindow", replayWindow);
        out.put("replayEvidenceDesk", replayEvidenceDesk);
        out.put("receiptCorrelationMode", receiptCorrelationMode);
        out.put("receiptDeadlineMode", receiptDeadlineMode);
        out.put("deliveryAssuranceMode", deliveryAssuranceMode);
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
