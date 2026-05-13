package com.tcc.pjb.backend.service.processual.recursal.protocolo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecursalTransmissionProfile(
        String protocolDesk,
        String remessaDesk,
        String autuacaoDesk,
        String integrationChannel,
        String credentialMode,
        String payloadPolicy,
        String transmissionMode,
        String queueSuffix,
        String reviewDesk,
        String ackDesk,
        String receiptChannel,
        String retryMode,
        String evidencePolicy,
        String complianceDesk,
        String protocolWindow,
        String connectorSystem,
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
        String competenceHint,
        boolean stepUpRequired,
        boolean certificateRequired,
        List<String> connectorWarnings,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public RecursalTransmissionProfile {
        connectorWarnings = connectorWarnings == null ? List.of() : List.copyOf(connectorWarnings);
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(protocolDesk, "PROTOCOLO"),
                firstNonBlank(remessaDesk, "REMESSA"),
                firstNonBlank(transmissionMode, "MODO"),
                firstNonBlank(integrationChannel, "CANAL"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("protocolDesk", protocolDesk);
        out.put("remessaDesk", remessaDesk);
        out.put("autuacaoDesk", autuacaoDesk);
        out.put("integrationChannel", integrationChannel);
        out.put("credentialMode", credentialMode);
        out.put("payloadPolicy", payloadPolicy);
        out.put("transmissionMode", transmissionMode);
        out.put("queueSuffix", queueSuffix);
        out.put("reviewDesk", reviewDesk);
        out.put("ackDesk", ackDesk);
        out.put("receiptChannel", receiptChannel);
        out.put("retryMode", retryMode);
        out.put("evidencePolicy", evidencePolicy);
        out.put("complianceDesk", complianceDesk);
        out.put("protocolWindow", protocolWindow);
        out.put("connectorSystem", connectorSystem);
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
        out.put("competenceHint", competenceHint);
        out.put("stepUpRequired", stepUpRequired);
        out.put("certificateRequired", certificateRequired);
        out.put("connectorWarnings", connectorWarnings);
        out.put("labels", labels);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }


    public static RecursalTransmissionProfile fallback(String payloadPolicy,
                                                       String competenceHint,
                                                       boolean stepUpRequired,
                                                       boolean certificateRequired) {
        return new RecursalTransmissionProfile(
                "PROTOCOLO_RECURSAL",
                "REMESSA_RECURSAL",
                "AUTUACAO_DESTINO",
                "INTERNO",
                "PADRAO",
                payloadPolicy,
                "SINCRONO",
                null,
                null,
                null,
                null,
                "PADRAO",
                "BASICA",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "TELEMETRIA_PADRAO",
                "AUDIT",
                null,
                null,
                "AUDIT",
                null,
                null,
                null,
                null,
                null,
                competenceHint,
                stepUpRequired,
                certificateRequired,
                List.of(),
                List.of("TRANSMISSION_FALLBACK"),
                new LinkedHashMap<>()
        );
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
