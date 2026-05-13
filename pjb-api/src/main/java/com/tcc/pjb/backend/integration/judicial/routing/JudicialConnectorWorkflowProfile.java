package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorWorkflowProfile(
        String tribunalCodigo,
        String tribunalNome,
        String connectorSystem,
        String protocolDesk,
        String dispatchDesk,
        String workflowMode,
        String competenceHint,
        String connectorBaseUrl,
        String receiptChannel,
        String evidenceEnvelope,
        String submissionWindow,
        boolean stepUpRequired,
        boolean certificateRequired,
        List<String> warnings,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public JudicialConnectorWorkflowProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(connectorSystem, "OUTRO"),
                firstNonBlank(protocolDesk, "PROTOCOLO"),
                firstNonBlank(workflowMode, "MODO"),
                firstNonBlank(receiptChannel, "ACK"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunalNome", tribunalNome);
        out.put("connectorSystem", connectorSystem);
        out.put("protocolDesk", protocolDesk);
        out.put("dispatchDesk", dispatchDesk);
        out.put("workflowMode", workflowMode);
        out.put("competenceHint", competenceHint);
        out.put("connectorBaseUrl", connectorBaseUrl);
        out.put("receiptChannel", receiptChannel);
        out.put("evidenceEnvelope", evidenceEnvelope);
        out.put("submissionWindow", submissionWindow);
        out.put("stepUpRequired", stepUpRequired);
        out.put("certificateRequired", certificateRequired);
        out.put("warnings", warnings);
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
