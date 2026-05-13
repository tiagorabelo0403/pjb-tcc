package com.tcc.pjb.backend.integration.judicial.routing;

import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorWorkflowResolver {

    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;

    public JudicialConnectorWorkflowResolver(TribunalProtocolRoutingService tribunalProtocolRoutingService) {
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
    }

    public JudicialConnectorWorkflowProfile resolve(Map<String, Object> payload,
                                                    String ritoName,
                                                    String ramoDireito,
                                                    String competencia,
                                                    boolean recurso,
                                                    boolean segredo,
                                                    boolean urgente) {
        LinkedHashMap<String, Object> safePayload = new LinkedHashMap<>();
        if (payload != null) {
            safePayload.putAll(payload);
        }
        TribunalProtocolRoutingService.RoutingDecision routing = tribunalProtocolRoutingService.resolve(safePayload, ritoName, ramoDireito, competencia, recurso);
        JudicialSubmissionCapability capability = routing.capability();
        String connectorSystem = routing.judicialSystem() == null ? JudicialSystem.OUTRO.name() : routing.judicialSystem().name();
        String protocolDesk = protocolDesk(connectorSystem);
        String dispatchDesk = segredo
                ? "DESPACHO_SIGILO_" + normalizeToken(routing.tribunalCodigo(), "TRIBUNAL")
                : urgente
                ? "DESPACHO_PRIORITARIO_" + normalizeToken(routing.tribunalCodigo(), "TRIBUNAL")
                : "DESPACHO_" + normalizeToken(connectorSystem, "OUTRO");
        String workflowMode = !capability.operational()
                ? "CONTINGENCIA_ASSISTIDA"
                : routing.stepUpRequired()
                ? "STEP_UP_GOVBR"
                : routing.certificateRequired()
                ? "CERTIFICADO_JUDICIAL"
                : recurso
                ? "PROTOCOLO_RECURSAL_ORQUESTRADO"
                : "PROTOCOLO_DISTRIBUICAO_ORQUESTRADO";
        String receiptChannel = capability.supportsEventSync()
                ? normalizeToken(connectorSystem, "OUTRO") + "_EVENT_ACK"
                : capability.supportsSnapshotSync()
                ? normalizeToken(connectorSystem, "OUTRO") + "_SNAPSHOT_ACK"
                : "ACK_MANUAL";
        String evidenceEnvelope = segredo
                ? "EVIDENCIA_MINIMA"
                : capability.supportsExternalMedia()
                ? "EVIDENCIA_COMPLETA_COM_MIDIA"
                : "EVIDENCIA_DOCUMENTAL";
        String submissionWindow = urgente
                ? "JANELA_IMEDIATA"
                : recurso
                ? "JANELA_RECURSAL"
                : "JANELA_FORUM";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(normalizeToken(connectorSystem, "OUTRO"));
        labels.add(workflowMode);
        if (routing.stepUpRequired()) {
            labels.add("STEP_UP_REQUIRED");
        }
        if (routing.certificateRequired()) {
            labels.add("CERTIFICATE_REQUIRED");
        }
        if (capability.operational()) {
            labels.add("CONNECTOR_OPERATIONAL");
        } else {
            labels.add("CONNECTOR_CONTINGENCY");
        }
        if (segredo) {
            labels.add("SEGREDO_JUSTICA");
        }
        if (urgente) {
            labels.add("PRIORIDADE_PROTOCOLO");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routingDecision", routing.metadata());
        metadata.put("capability", Map.of(
                "enabled", capability.enabled(),
                "supportsProtocol", capability.supportsProtocol(),
                "supportsDryRun", capability.supportsDryRun(),
                "supportsSnapshotSync", capability.supportsSnapshotSync(),
                "supportsEventSync", capability.supportsEventSync(),
                "requiresStepUpGovBr", capability.requiresStepUpGovBr(),
                "requiresCertificate", capability.requiresCertificate(),
                "supportsExternalMedia", capability.supportsExternalMedia()
        ));
        metadata.put("descriptor", protocolDesk + ':' + dispatchDesk + ':' + workflowMode);

        return new JudicialConnectorWorkflowProfile(
                routing.tribunalCodigo(),
                routing.tribunalNome(),
                connectorSystem,
                protocolDesk,
                dispatchDesk,
                workflowMode,
                routing.competenceHint(),
                capability.baseUrl(),
                receiptChannel,
                evidenceEnvelope,
                submissionWindow,
                routing.stepUpRequired(),
                routing.certificateRequired(),
                routing.warnings(),
                List.copyOf(labels),
                metadata
        );
    }

    private static String protocolDesk(String connectorSystem) {
        return switch (normalizeToken(connectorSystem, "OUTRO")) {
            case "PJE" -> "PROTOCOLO_PJE";
            case "EPROC" -> "PROTOCOLO_EPROC";
            case "ESAJ" -> "PROTOCOLO_ESAJ";
            case "PROJUDI" -> "PROTOCOLO_PROJUDI";
            case "CRETA" -> "PROTOCOLO_CRETA";
            default -> "PROTOCOLO_ORQUESTRADO";
        };
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
