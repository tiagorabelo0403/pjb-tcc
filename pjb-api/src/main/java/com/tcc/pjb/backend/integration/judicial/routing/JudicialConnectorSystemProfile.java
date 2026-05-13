package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorSystemProfile(
        String systemKey,
        String protocolNamespace,
        String receiptPattern,
        String replayStrategy,
        String evidenceStore,
        String contingencySystem,
        String manualChannel,
        String publicationBridge,
        boolean sessionAware,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public JudicialConnectorSystemProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(systemKey, "OUTRO"),
                firstNonBlank(protocolNamespace, "PROTOCOLO"),
                firstNonBlank(receiptPattern, "ACK"),
                firstNonBlank(replayStrategy, "REPLAY"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("systemKey", systemKey);
        out.put("protocolNamespace", protocolNamespace);
        out.put("receiptPattern", receiptPattern);
        out.put("replayStrategy", replayStrategy);
        out.put("evidenceStore", evidenceStore);
        out.put("contingencySystem", contingencySystem);
        out.put("manualChannel", manualChannel);
        out.put("publicationBridge", publicationBridge);
        out.put("sessionAware", sessionAware);
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
