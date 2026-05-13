package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorSubmissionAuditProfile(
        String submissionAuditMode,
        String protocolSlaBucket,
        String escalationDesk,
        String receiptAuditDesk,
        String proofBundleMode,
        String reconciliationWindow,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public JudicialConnectorSubmissionAuditProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(submissionAuditMode, "AUDIT"),
                firstNonBlank(protocolSlaBucket, "SLA"),
                firstNonBlank(proofBundleMode, "PROOF"),
                firstNonBlank(reconciliationWindow, "WINDOW"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("submissionAuditMode", submissionAuditMode);
        out.put("protocolSlaBucket", protocolSlaBucket);
        out.put("escalationDesk", escalationDesk);
        out.put("receiptAuditDesk", receiptAuditDesk);
        out.put("proofBundleMode", proofBundleMode);
        out.put("reconciliationWindow", reconciliationWindow);
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
