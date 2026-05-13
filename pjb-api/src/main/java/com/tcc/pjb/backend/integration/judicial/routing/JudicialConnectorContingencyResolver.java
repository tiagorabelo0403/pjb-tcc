package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorContingencyResolver {

    public JudicialConnectorContingencyProfile resolve(String connectorSystem,
                                                       JudicialConnectorWorkflowProfile workflowProfile,
                                                       boolean segredo,
                                                       boolean urgente,
                                                       boolean recursal) {
        String workflowConnector = workflowProfile == null ? null : workflowProfile.connectorSystem();
        String normalizedConnector = normalizeToken(firstNonBlank(connectorSystem, workflowConnector), "OUTRO");
        boolean workflowMissing = workflowProfile == null;
        List<String> workflowWarnings = workflowMissing ? List.of() : workflowProfile.warnings();
        List<String> workflowLabels = workflowMissing ? List.of() : workflowProfile.labels();
        boolean manualAssistida = workflowMissing
                || !"OUTRO".equals(normalizedConnector) && (workflowProfile.stepUpRequired() || workflowProfile.certificateRequired())
                || workflowWarnings.stream().anyMatch(w -> w != null && !w.isBlank());
        boolean contingency = workflowMissing || !workflowLabels.contains("CONNECTOR_OPERATIONAL");
        String fallbackMode = contingency
                ? "CONTINGENCIA_ASSISTIDA_MANUAL"
                : manualAssistida
                ? "PROTOCOLO_ASSISTIDO_SUPERVISIONADO"
                : urgente
                ? "PROTOCOLO_DIRETO_PRIORITARIO"
                : recursal
                ? "PROTOCOLO_RECURSAL_CONFIRMADO"
                : "PROTOCOLO_DIRETO_CONFIRMADO";
        String contingencyDesk = segredo
                ? "CONTINGENCIA_SIGILO_" + normalizedConnector
                : urgente
                ? "CONTINGENCIA_PRIORIDADE_" + normalizedConnector
                : "CONTINGENCIA_" + normalizedConnector;
        String replayQueue = segredo
                ? "REPLAY_SIGILO_" + normalizedConnector
                : recursal
                ? "REPLAY_RECURSAL_" + normalizedConnector
                : "REPLAY_PROTOCOLO_" + normalizedConnector;
        String evidenceRetentionPolicy = segredo
                ? "RETENCAO_REFORCADA_365D"
                : recursal
                ? "RETENCAO_RECURSAL_180D"
                : "RETENCAO_PADRAO_120D";
        String manualSubmissionDesk = workflowProfile != null && workflowProfile.stepUpRequired()
                ? "PROTOCOLO_STEP_UP_" + normalizedConnector
                : workflowProfile != null && workflowProfile.certificateRequired()
                ? "PROTOCOLO_CERTIFICADO_" + normalizedConnector
                : "PROTOCOLO_ASSISTIDO_" + normalizedConnector;
        String receiptGuaranteeMode = contingency
                ? "RECIBO_MANUAL_EVIDENCIADO"
                : workflowProfile != null && "ACK_MANUAL".equalsIgnoreCase(firstNonBlank(workflowProfile.receiptChannel(), null))
                ? "RECIBO_HIBRIDO"
                : "RECIBO_CONFIRMADO_AUTOMATICO";
        String contingencyWindow = urgente
                ? "JANELA_CONTINGENCIA_IMEDIATA"
                : segredo
                ? "JANELA_CONTINGENCIA_CONTROLADA"
                : "JANELA_CONTINGENCIA_OPERACIONAL";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(fallbackMode);
        labels.add(receiptGuaranteeMode);
        labels.add(contingencyWindow);
        if (contingency) {
            labels.add("CONNECTOR_CONTINGENCIA_FORCADA");
        }
        if (manualAssistida) {
            labels.add("PROTOCOLO_ASSISTIDO");
        }
        if (segredo) {
            labels.add("SEGREDO_CONTROLADO");
        }
        if (urgente) {
            labels.add("PRIORIDADE_OPERACIONAL");
        }
        if (recursal) {
            labels.add("RECURSAL_GUARDED_FLOW");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("connectorSystem", normalizedConnector);
        metadata.put("workflowDescriptor", workflowProfile == null ? null : workflowProfile.descriptor());
        metadata.put("workflowWarnings", workflowProfile == null ? List.of() : workflowProfile.warnings());
        metadata.put("manualAssistida", manualAssistida);
        metadata.put("contingency", contingency);
        metadata.put("descriptor", contingencyDesk + ':' + replayQueue + ':' + fallbackMode);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new JudicialConnectorContingencyProfile(
                fallbackMode,
                contingencyDesk,
                replayQueue,
                evidenceRetentionPolicy,
                manualSubmissionDesk,
                receiptGuaranteeMode,
                contingencyWindow,
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

    private static String normalizeToken(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
