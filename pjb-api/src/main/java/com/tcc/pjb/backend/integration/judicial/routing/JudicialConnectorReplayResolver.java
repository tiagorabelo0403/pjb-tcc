package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorReplayResolver {

    public JudicialConnectorReplayProfile resolve(JudicialConnectorSystemProfile systemProfile,
                                                  JudicialConnectorWorkflowProfile workflowProfile,
                                                  JudicialConnectorContingencyProfile contingencyProfile,
                                                  boolean segredo,
                                                  boolean urgente,
                                                  boolean recursal) {
        String system = normalize(firstNonBlank(
                systemProfile == null ? null : systemProfile.systemKey(),
                workflowProfile == null ? null : workflowProfile.connectorSystem(),
                contingencyProfile == null ? null : contingencyProfile.replayQueue()), "OUTRO");
        boolean manualFallback = contingencyProfile != null
                && contingencyProfile.fallbackMode() != null
                && contingencyProfile.fallbackMode().contains("MANUAL");
        String replayMode = switch (system) {
            case "PJE" -> manualFallback ? "REPLAY_EVENTO_ASSISTIDO" : "REPLAY_EVENTO_CONFIRMADO";
            case "EPROC" -> manualFallback ? "REPLAY_SNAPSHOT_ASSISTIDO" : "REPLAY_SNAPSHOT_CONFIRMADO";
            case "ESAJ" -> "REPLAY_PROTOCOLO_LOTE";
            case "PROJUDI" -> "REPLAY_FILA_AUDITAVEL";
            case "CRETA" -> "REPLAY_REMESSA_CONTROLADA";
            case "PJB_INTERNAL", "PJB_RECURSAL_BRIDGE", "PJB_DISTRIBUICAO_BRIDGE" -> "REPLAY_INTERNO_ORQUESTRADO";
            default -> manualFallback ? "REPLAY_MANUAL_SUPERVISIONADO" : "REPLAY_HIBRIDO_CONTROLADO";
        };
        String replayWindow = urgente
                ? "REPLAY_WINDOW_IMEDIATA"
                : segredo
                ? "REPLAY_WINDOW_RESTRITA"
                : recursal
                ? "REPLAY_WINDOW_RECURSAL"
                : "REPLAY_WINDOW_OPERACIONAL";
        String replayEvidenceDesk = segredo
                ? "EVIDENCIA_SIGILO_" + system
                : recursal
                ? "EVIDENCIA_RECURSAL_" + system
                : "EVIDENCIA_PROTOCOLO_" + system;
        String receiptCorrelationMode = switch (system) {
            case "PJE", "EPROC" -> "CORRELACAO_EVENTO_HASH";
            case "ESAJ", "PROJUDI", "CRETA" -> "CORRELACAO_RECIBO_PROTOCOLO";
            default -> "CORRELACAO_DESCRIPTOR_OPERACIONAL";
        };
        String receiptDeadlineMode = urgente
                ? "RECIBO_DEADLINE_CURTO"
                : manualFallback
                ? "RECIBO_DEADLINE_SUPERVISIONADO"
                : "RECIBO_DEADLINE_PADRAO";
        String deliveryAssuranceMode = manualFallback
                ? "ASSURANCE_DUPLA_CONFERENCIA"
                : segredo
                ? "ASSURANCE_EVIDENCIA_REFORCADA"
                : recursal
                ? "ASSURANCE_RECIBO_E_REPLAY"
                : "ASSURANCE_ACK_EVIDENCIADO";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(replayMode);
        labels.add(receiptCorrelationMode);
        labels.add(deliveryAssuranceMode);
        labels.add(replayWindow);
        if (manualFallback) {
            labels.add("REPLAY_MANUAL_FALLBACK");
        }
        if (segredo) {
            labels.add("REPLAY_SIGILO_CONTROLADO");
        }
        if (urgente) {
            labels.add("REPLAY_PRIORITARIO");
        }
        if (recursal) {
            labels.add("REPLAY_RECURSAL");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("system", system);
        metadata.put("systemDescriptor", systemProfile == null ? null : systemProfile.descriptor());
        metadata.put("workflowDescriptor", workflowProfile == null ? null : workflowProfile.descriptor());
        metadata.put("contingencyDescriptor", contingencyProfile == null ? null : contingencyProfile.descriptor());
        metadata.put("manualFallback", manualFallback);
        metadata.put("descriptor", replayMode + ':' + replayEvidenceDesk + ':' + deliveryAssuranceMode);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new JudicialConnectorReplayProfile(
                replayMode,
                replayWindow,
                replayEvidenceDesk,
                receiptCorrelationMode,
                receiptDeadlineMode,
                deliveryAssuranceMode,
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
