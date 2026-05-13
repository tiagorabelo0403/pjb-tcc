package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorTelemetryResolver {

    public JudicialConnectorTelemetryProfile resolve(JudicialConnectorSystemProfile systemProfile,
                                                     JudicialConnectorWorkflowProfile workflowProfile,
                                                     JudicialConnectorContingencyProfile contingencyProfile,
                                                     JudicialConnectorReplayProfile replayProfile,
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
        String telemetryMode = manualFallback
                ? "TELEMETRIA_CONTINGENCIA_ASSISTIDA"
                : recursal
                ? "TELEMETRIA_RECURSAL_ORQUESTRADA"
                : "TELEMETRIA_PROTOCOLO_ORQUESTRADA";
        String telemetryChannel = switch (system) {
            case "PJE" -> "OTEL_PJE_EVENT_STREAM";
            case "EPROC" -> "OTEL_EPROC_SNAPSHOT_STREAM";
            case "ESAJ" -> "OTEL_ESAJ_PROTOCOLO_STREAM";
            case "PROJUDI" -> "OTEL_PROJUDI_QUEUE_STREAM";
            case "CRETA" -> "OTEL_CRETA_REMESSA_STREAM";
            case "PJB_INTERNAL", "PJB_RECURSAL_BRIDGE", "PJB_DISTRIBUICAO_BRIDGE" -> "OTEL_PJB_INTERNAL_STREAM";
            default -> "OTEL_JUDICIAL_GENERIC_STREAM";
        };
        String deadLetterQueue = firstNonBlank(
                contingencyProfile == null ? null : contingencyProfile.replayQueue(),
                systemProfile == null ? null : systemProfile.protocolNamespace(),
                "DLQ_" + system);
        String reconciliationDesk = segredo
                ? "RECONCILIACAO_SIGILO_" + system
                : recursal
                ? "RECONCILIACAO_RECURSAL_" + system
                : "RECONCILIACAO_PROTOCOLO_" + system;
        String observabilityPolicy = urgente
                ? "OBSERVABILIDADE_TEMPO_REAL"
                : segredo
                ? "OBSERVABILIDADE_RESTRITA_AUDITAVEL"
                : manualFallback
                ? "OBSERVABILIDADE_ASSISTIDA"
                : "OBSERVABILIDADE_PADRAO_AUDITAVEL";
        String correlationKeyMode = firstNonBlank(
                replayProfile == null ? null : replayProfile.receiptCorrelationMode(),
                systemProfile == null ? null : systemProfile.receiptPattern(),
                "CORRELACAO_OPERACIONAL");

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(telemetryMode);
        labels.add(observabilityPolicy);
        labels.add(correlationKeyMode);
        if (manualFallback) {
            labels.add("TELEMETRY_MANUAL_FALLBACK");
        }
        if (segredo) {
            labels.add("TELEMETRY_SIGILO");
        }
        if (urgente) {
            labels.add("TELEMETRY_REALTIME");
        }
        if (recursal) {
            labels.add("TELEMETRY_RECURSAL");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("system", system);
        metadata.put("manualFallback", manualFallback);
        metadata.put("workflowDescriptor", workflowProfile == null ? null : workflowProfile.descriptor());
        metadata.put("contingencyDescriptor", contingencyProfile == null ? null : contingencyProfile.descriptor());
        metadata.put("replayDescriptor", replayProfile == null ? null : replayProfile.descriptor());
        metadata.put("descriptor", telemetryMode + ':' + telemetryChannel + ':' + observabilityPolicy);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new JudicialConnectorTelemetryProfile(
                telemetryMode,
                telemetryChannel,
                deadLetterQueue,
                reconciliationDesk,
                observabilityPolicy,
                correlationKeyMode,
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
