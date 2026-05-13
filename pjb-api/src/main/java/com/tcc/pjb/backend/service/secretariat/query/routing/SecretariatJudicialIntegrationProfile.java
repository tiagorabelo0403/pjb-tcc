package com.tcc.pjb.backend.service.secretariat.query.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SecretariatJudicialIntegrationProfile(
        String targetSystem,
        String protocolDesk,
        String dispatchChannel,
        String credentialMode,
        String payloadPolicy,
        String syncMode,
        String externalQueueSuffix,
        String reviewDesk,
        String connectorId,
        String ackChannel,
        String replayDesk,
        String retryMode,
        String evidencePolicy,
        String dispatchWindow,
        String tribunalCodigo,
        String tribunalNome,
        String connectorSystem,
        String competenceHint,
        String connectorBaseUrl,
        String connectorWorkflowMode,
        String fallbackMode,
        String contingencyDesk,
        String replayQueue,
        String evidenceRetentionPolicy,
        String manualSubmissionDesk,
        String telemetryMode,
        String telemetryChannel,
        String deadLetterQueue,
        String reconciliationDesk,
        String submissionAuditMode,
        String protocolSlaBucket,
        String escalationDesk,
        String receiptAuditDesk,
        String proofBundleMode,
        String reconciliationWindow,
        boolean stepUpRequired,
        boolean certificateRequired,
        List<String> connectorWarnings,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public SecretariatJudicialIntegrationProfile {
        connectorWarnings = connectorWarnings == null ? List.of() : List.copyOf(connectorWarnings);
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(targetSystem, "PJB"),
                firstNonBlank(protocolDesk, "PROTOCOLO"),
                firstNonBlank(syncMode, "SYNC"),
                firstNonBlank(dispatchChannel, "CHANNEL"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("targetSystem", targetSystem);
        out.put("protocolDesk", protocolDesk);
        out.put("dispatchChannel", dispatchChannel);
        out.put("credentialMode", credentialMode);
        out.put("payloadPolicy", payloadPolicy);
        out.put("syncMode", syncMode);
        out.put("externalQueueSuffix", externalQueueSuffix);
        out.put("reviewDesk", reviewDesk);
        out.put("connectorId", connectorId);
        out.put("ackChannel", ackChannel);
        out.put("replayDesk", replayDesk);
        out.put("retryMode", retryMode);
        out.put("evidencePolicy", evidencePolicy);
        out.put("dispatchWindow", dispatchWindow);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunalNome", tribunalNome);
        out.put("connectorSystem", connectorSystem);
        out.put("competenceHint", competenceHint);
        out.put("connectorBaseUrl", connectorBaseUrl);
        out.put("connectorWorkflowMode", connectorWorkflowMode);
        out.put("fallbackMode", fallbackMode);
        out.put("contingencyDesk", contingencyDesk);
        out.put("replayQueue", replayQueue);
        out.put("evidenceRetentionPolicy", evidenceRetentionPolicy);
        out.put("manualSubmissionDesk", manualSubmissionDesk);
        out.put("telemetryMode", telemetryMode);
        out.put("telemetryChannel", telemetryChannel);
        out.put("deadLetterQueue", deadLetterQueue);
        out.put("reconciliationDesk", reconciliationDesk);
        out.put("submissionAuditMode", submissionAuditMode);
        out.put("protocolSlaBucket", protocolSlaBucket);
        out.put("escalationDesk", escalationDesk);
        out.put("receiptAuditDesk", receiptAuditDesk);
        out.put("proofBundleMode", proofBundleMode);
        out.put("reconciliationWindow", reconciliationWindow);
        out.put("stepUpRequired", stepUpRequired);
        out.put("certificateRequired", certificateRequired);
        out.put("connectorWarnings", connectorWarnings);
        out.put("labels", labels);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
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
