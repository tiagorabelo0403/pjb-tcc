package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorSubmissionAuditResolver {

    public JudicialConnectorSubmissionAuditProfile resolve(JudicialConnectorSystemProfile systemProfile,
                                                           JudicialConnectorWorkflowProfile workflowProfile,
                                                           JudicialConnectorContingencyProfile contingencyProfile,
                                                           JudicialConnectorReplayProfile replayProfile,
                                                           JudicialConnectorTelemetryProfile telemetryProfile,
                                                           boolean segredo,
                                                           boolean urgente,
                                                           boolean recursal) {
        String system = normalize(firstNonBlank(
                systemProfile == null ? null : systemProfile.systemKey(),
                workflowProfile == null ? null : workflowProfile.connectorSystem(),
                "OUTRO"), "OUTRO");
        boolean manualFallback = contingencyProfile != null
                && contingencyProfile.fallbackMode() != null
                && contingencyProfile.fallbackMode().contains("MANUAL");
        String submissionAuditMode = manualFallback
                ? "AUDITORIA_SUBMISSAO_ASSISTIDA"
                : recursal
                ? "AUDITORIA_SUBMISSAO_RECURSAL"
                : "AUDITORIA_SUBMISSAO_PROTOCOLO";
        String protocolSlaBucket = urgente
                ? "SLA_IMEDIATO"
                : segredo
                ? "SLA_RESTRITO"
                : recursal
                ? "SLA_RECURSAL_MONITORADO"
                : "SLA_PROTOCOLO_PADRAO";
        String escalationDesk = segredo
                ? "ESCALACAO_SIGILO_" + system
                : urgente
                ? "ESCALACAO_PRIORITARIA_" + system
                : recursal
                ? "ESCALACAO_RECURSAL_" + system
                : "ESCALACAO_PROTOCOLO_" + system;
        String receiptAuditDesk = firstNonBlank(
                telemetryProfile == null ? null : telemetryProfile.reconciliationDesk(),
                replayProfile == null ? null : replayProfile.replayEvidenceDesk(),
                "AUDITORIA_RECIBO_" + system);
        String proofBundleMode = segredo
                ? "DOSSIE_MINIMIZADO_AUDITAVEL"
                : manualFallback
                ? "DOSSIE_CONTINGENCIA_ASSISTIDA"
                : recursal
                ? "DOSSIE_RECURSAL_CORRELACIONADO"
                : "DOSSIE_PROTOCOLO_CORRELACIONADO";
        String reconciliationWindow = urgente
                ? "JANELA_30M"
                : recursal
                ? "JANELA_4H"
                : "JANELA_D1";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(submissionAuditMode);
        labels.add(protocolSlaBucket);
        labels.add(proofBundleMode);
        labels.add(reconciliationWindow);
        if (manualFallback) {
            labels.add("AUDIT_MANUAL_FALLBACK");
        }
        if (segredo) {
            labels.add("AUDIT_SIGILO");
        }
        if (urgente) {
            labels.add("AUDIT_PRIORITARIO");
        }
        if (recursal) {
            labels.add("AUDIT_RECURSAL");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("system", system);
        metadata.put("manualFallback", manualFallback);
        metadata.put("workflowDescriptor", workflowProfile == null ? null : workflowProfile.descriptor());
        metadata.put("replayDescriptor", replayProfile == null ? null : replayProfile.descriptor());
        metadata.put("telemetryDescriptor", telemetryProfile == null ? null : telemetryProfile.descriptor());
        metadata.put("descriptor", submissionAuditMode + ':' + protocolSlaBucket + ':' + proofBundleMode);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new JudicialConnectorSubmissionAuditProfile(
                submissionAuditMode,
                protocolSlaBucket,
                escalationDesk,
                receiptAuditDesk,
                proofBundleMode,
                reconciliationWindow,
                List.copyOf(labels),
                metadata
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

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
