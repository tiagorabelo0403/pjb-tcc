package com.tcc.pjb.backend.service.secretariat.query.routing;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorContingencyProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorContingencyResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorWorkflowProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorWorkflowResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSystemProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSystemResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorReplayProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorReplayResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorTelemetryProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorTelemetryResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSubmissionAuditProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSubmissionAuditResolver;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SecretariatJudicialIntegrationResolver {

    private final SecretariatConnectorDispatchResolver connectorDispatchResolver;
    private final JudicialConnectorWorkflowResolver connectorWorkflowResolver;
    private final JudicialConnectorContingencyResolver connectorContingencyResolver;
    private final JudicialConnectorSystemResolver connectorSystemResolver;
    private final JudicialConnectorReplayResolver connectorReplayResolver;
    private final JudicialConnectorTelemetryResolver connectorTelemetryResolver;
    private final JudicialConnectorSubmissionAuditResolver submissionAuditResolver;

    public SecretariatJudicialIntegrationResolver(SecretariatConnectorDispatchResolver connectorDispatchResolver,
                                                  JudicialConnectorWorkflowResolver connectorWorkflowResolver,
                                                  JudicialConnectorContingencyResolver connectorContingencyResolver,
                                                  JudicialConnectorSystemResolver connectorSystemResolver,
                                                  JudicialConnectorReplayResolver connectorReplayResolver,
                                                  JudicialConnectorTelemetryResolver connectorTelemetryResolver,
                                                  JudicialConnectorSubmissionAuditResolver submissionAuditResolver) {
        this.connectorDispatchResolver = connectorDispatchResolver;
        this.connectorWorkflowResolver = connectorWorkflowResolver;
        this.connectorContingencyResolver = connectorContingencyResolver;
        this.connectorSystemResolver = connectorSystemResolver;
        this.connectorReplayResolver = connectorReplayResolver;
        this.connectorTelemetryResolver = connectorTelemetryResolver;
        this.submissionAuditResolver = submissionAuditResolver;
    }

    public SecretariatJudicialIntegrationProfile resolve(String inboxKey,
                                                         String queueCode,
                                                         String title,
                                                         Collection<String> tags,
                                                         ForumDeskPortfolioProfile portfolio,
                                                         SecretariatFlowBridgeProfile bridgeProfile) {
        String source = ((inboxKey == null ? "" : inboxKey) + ' '
                + (queueCode == null ? "" : queueCode) + ' '
                + (title == null ? "" : title) + ' '
                + (tags == null ? "" : String.join(" ", tags)))
                .toUpperCase(Locale.ROOT);

        String inferredTribunalCodigo = inferTribunalCodigo(inboxKey, title, tags);
        String inferredUf = SecretariatInboxKeyParser.parse(inboxKey).map(SecretariatInboxKeyParser.Parts::uf).orElse(null);
        String inferredComarca = SecretariatInboxKeyParser.parse(inboxKey).map(SecretariatInboxKeyParser.Parts::comarca).orElse(null);
        String ramoDireito = inferRamo(source, bridgeProfile);
        String competencia = inferCompetencia(source, bridgeProfile);
        boolean segredo = containsAny(source, "SIGILO", "SEGREDO", "RESTRITO", "CREDENCIAL");
        boolean urgente = containsAny(source, "HC", "LIMINAR", "URGENT", "UTI", "MEDICAMENTO", "PLANTAO");
        Map<String, Object> routingPayload = new LinkedHashMap<>();
        routingPayload.put("tribunalCodigo", inferredTribunalCodigo);
        routingPayload.put("tribunal", inferredTribunalCodigo);
        routingPayload.put("uf", inferredUf);
        routingPayload.put("comarca", inferredComarca);
        JudicialConnectorWorkflowProfile workflowProfile = connectorWorkflowResolver.resolve(
                routingPayload,
                null,
                ramoDireito,
                competencia,
                bridgeProfile != null && bridgeProfile.requiresRecursalSync(),
                segredo,
                urgente
        );
        JudicialConnectorContingencyProfile contingencyProfile = connectorContingencyResolver.resolve(
                workflowProfile.connectorSystem(),
                workflowProfile,
                segredo,
                urgente,
                bridgeProfile != null && bridgeProfile.requiresRecursalSync()
        );

        String targetSystem = containsAny(source, "EPROC") ? "EPROC"
                : containsAny(source, "PROJUDI") ? "PROJUDI"
                : containsAny(source, "ESAJ") ? "ESAJ"
                : containsAny(source, "CRETA") ? "CRETA"
                : containsAny(source, "PJE", "PJECALC", "PJe") ? "PJE"
                : workflowProfile.connectorSystem() != null && !"OUTRO".equals(workflowProfile.connectorSystem()) ? workflowProfile.connectorSystem()
                : bridgeProfile != null && bridgeProfile.requiresRecursalSync() ? "PJB_RECURSAL_BRIDGE"
                : bridgeProfile != null && bridgeProfile.requiresDistributionSync() ? "PJB_DISTRIBUICAO_BRIDGE"
                : "PJB_INTERNAL";

        JudicialConnectorSystemProfile systemProfile = connectorSystemResolver.resolve(
                targetSystem,
                workflowProfile.tribunalCodigo(),
                workflowProfile.competenceHint(),
                segredo,
                urgente,
                bridgeProfile != null && bridgeProfile.requiresRecursalSync()
        );
        JudicialConnectorReplayProfile replayProfile = connectorReplayResolver.resolve(
                systemProfile,
                workflowProfile,
                contingencyProfile,
                segredo,
                urgente,
                bridgeProfile != null && bridgeProfile.requiresRecursalSync()
        );
        JudicialConnectorTelemetryProfile telemetryProfile = connectorTelemetryResolver.resolve(
                systemProfile,
                workflowProfile,
                contingencyProfile,
                replayProfile,
                segredo,
                urgente,
                bridgeProfile != null && bridgeProfile.requiresRecursalSync()
        );
        JudicialConnectorSubmissionAuditProfile submissionAuditProfile = submissionAuditResolver.resolve(
                systemProfile,
                workflowProfile,
                contingencyProfile,
                replayProfile,
                telemetryProfile,
                segredo,
                urgente,
                bridgeProfile != null && bridgeProfile.requiresRecursalSync()
        );

        String protocolDesk = switch (targetSystem) {
            case "PJE" -> "PROTOCOLO_PJE";
            case "EPROC" -> "PROTOCOLO_EPROC";
            case "ESAJ" -> "PROTOCOLO_ESAJ";
            case "PROJUDI" -> "PROTOCOLO_PROJUDI";
            case "CRETA" -> "PROTOCOLO_CRETA";
            default -> portfolio == null ? "PROTOCOLO_PJB" : firstNonBlank(portfolio.complianceDesk(), "PROTOCOLO_PJB");
        };
        String dispatchChannel = bridgeProfile != null && bridgeProfile.requiresRecursalSync() ? "REC_BRIDGE_CHANNEL"
                : bridgeProfile != null && bridgeProfile.requiresGabineteSync() ? "GAB_BRIDGE_CHANNEL"
                : bridgeProfile != null && bridgeProfile.requiresDistributionSync() ? "DIST_BRIDGE_CHANNEL"
                : firstNonBlank(workflowProfile.dispatchDesk(), systemProfile.protocolNamespace(), "SECRETARIA_CHANNEL");
        String credentialMode = containsAny(source, "SIGILO", "SEGREDO", "RESTRITO", "CREDENCIAL")
                ? "CREDENCIAL_REFORCADA"
                : containsAny(source, "HC", "LIMINAR", "URGENT", "UTI", "MEDICAMENTO")
                ? "CREDENCIAL_PRIORITARIA"
                : "CREDENCIAL_PADRAO";
        String payloadPolicy = "CREDENCIAL_REFORCADA".equals(credentialMode) ? "PAYLOAD_MINIMIZADO"
                : containsAny(source, "PDF", "ANEXO", "MIDIA", "ARQUIVO") ? "PACOTE_DOCUMENTAL_COMPLETO"
                : systemProfile.evidenceStore() != null && systemProfile.evidenceStore().contains("DOSSIE") ? "PACOTE_AUDITAVEL"
                : "PACOTE_RESUMIDO";
        String syncMode = targetSystem.equals("PJB_INTERNAL") ? "SINCRONIA_INTERNA"
                : workflowProfile.stepUpRequired() || workflowProfile.certificateRequired() ? "SINCRONIA_ASSISTIDA"
                : bridgeProfile != null && bridgeProfile.requiresDistributionSync() ? "SINCRONIA_PROTOCOLO_DISTRIBUICAO"
                : bridgeProfile != null && bridgeProfile.requiresRecursalSync() ? "SINCRONIA_RECURSAL"
                : "SINCRONIA_PROTOCOLO";
        String externalQueueSuffix = (targetSystem + '_' + dispatchChannel + '_' + syncMode).replace(' ', '_');
        String reviewDesk = "CREDENCIAL_REFORCADA".equals(credentialMode)
                ? "REVISAO_SIGILO_PROTOCOLO"
                : bridgeProfile != null && bridgeProfile.admissibilityDesk() != null
                ? bridgeProfile.admissibilityDesk()
                : portfolio == null ? "REVISAO_PROTOCOLO" : firstNonBlank(portfolio.assistantDesk(), "REVISAO_PROTOCOLO");

        SecretariatConnectorDispatchProfile connectorProfile = connectorDispatchResolver.resolve(targetSystem, title, tags, portfolio, bridgeProfile);

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(targetSystem);
        labels.add(syncMode);
        labels.add(dispatchChannel);
        labels.add(payloadPolicy);
        if (!"PJB_INTERNAL".equals(targetSystem)) {
            labels.add("EXTERNAL_SYNC");
        }
        if (bridgeProfile != null && bridgeProfile.requiresRecursalSync()) {
            labels.add("RECURSAL_SYNC");
        }
        if (bridgeProfile != null && bridgeProfile.requiresDistributionSync()) {
            labels.add("DISTRIBUICAO_SYNC");
        }
        labels.addAll(connectorProfile.labels());
        labels.addAll(systemProfile.labels());
        labels.addAll(workflowProfile.labels());
        labels.addAll(contingencyProfile.labels());
        labels.addAll(replayProfile.labels());
        labels.addAll(telemetryProfile.labels());
        labels.addAll(submissionAuditProfile.labels());

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("inboxKey", inboxKey);
        metadata.put("queueCode", queueCode);
        metadata.put("title", title);
        metadata.putAll(connectorProfile.toMap());
        metadata.putAll(systemProfile.toMap());
        metadata.put("replay", replayProfile.toMap());
        metadata.put("telemetry", telemetryProfile.toMap());
        metadata.put("submissionAudit", submissionAuditProfile.toMap());
        metadata.putAll(workflowProfile.toMap());
        metadata.putAll(contingencyProfile.toMap());
        metadata.put("systemDescriptor", systemProfile.descriptor());
        metadata.put("contingencyDescriptor", contingencyProfile.descriptor());
        metadata.put("replayDescriptor", replayProfile.descriptor());
        metadata.put("telemetryDescriptor", telemetryProfile.descriptor());
        metadata.put("submissionAuditDescriptor", submissionAuditProfile.descriptor());
        metadata.put("descriptor", targetSystem + ':' + protocolDesk + ':' + syncMode);

        return new SecretariatJudicialIntegrationProfile(
                targetSystem,
                firstNonBlank(workflowProfile.protocolDesk(), protocolDesk),
                dispatchChannel,
                credentialMode,
                payloadPolicy,
                syncMode,
                externalQueueSuffix,
                reviewDesk,
                connectorProfile.connectorId(),
                firstNonBlank(workflowProfile.receiptChannel(), connectorProfile.ackChannel(), telemetryProfile.telemetryChannel(), replayProfile.receiptCorrelationMode(), systemProfile.receiptPattern()),
                firstNonBlank(connectorProfile.replayDesk(), telemetryProfile.reconciliationDesk(), replayProfile.replayEvidenceDesk()),
                firstNonBlank(connectorProfile.retryMode(), replayProfile.replayMode(), telemetryProfile.deadLetterQueue(), systemProfile.replayStrategy()),
                firstNonBlank(workflowProfile.evidenceEnvelope(), connectorProfile.evidencePolicy(), telemetryProfile.observabilityPolicy(), replayProfile.deliveryAssuranceMode(), systemProfile.evidenceStore()),
                firstNonBlank(workflowProfile.submissionWindow(), connectorProfile.dispatchWindow()),
                workflowProfile.tribunalCodigo(),
                workflowProfile.tribunalNome(),
                workflowProfile.connectorSystem(),
                workflowProfile.competenceHint(),
                workflowProfile.connectorBaseUrl(),
                workflowProfile.workflowMode(),
                contingencyProfile.fallbackMode(),
                contingencyProfile.contingencyDesk(),
                contingencyProfile.replayQueue(),
                contingencyProfile.evidenceRetentionPolicy(),
                contingencyProfile.manualSubmissionDesk(),
                telemetryProfile.telemetryMode(),
                telemetryProfile.telemetryChannel(),
                telemetryProfile.deadLetterQueue(),
                telemetryProfile.reconciliationDesk(),
                submissionAuditProfile.submissionAuditMode(),
                submissionAuditProfile.protocolSlaBucket(),
                submissionAuditProfile.escalationDesk(),
                submissionAuditProfile.receiptAuditDesk(),
                submissionAuditProfile.proofBundleMode(),
                submissionAuditProfile.reconciliationWindow(),
                workflowProfile.stepUpRequired(),
                workflowProfile.certificateRequired(),
                workflowProfile.warnings(),
                List.copyOf(labels),
                metadata
        );
    }

    private static boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && source.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }


    private static String inferTribunalCodigo(String inboxKey, String title, Collection<String> tags) {
        String source = ((inboxKey == null ? "" : inboxKey) + ' ' + (title == null ? "" : title) + ' ' + (tags == null ? "" : String.join(" ", tags))).toUpperCase(Locale.ROOT);
        return SecretariatInboxKeyParser.parse(inboxKey)
                .map(SecretariatInboxKeyParser.Parts::org)
                .filter(org -> org != null && !org.isBlank())
                .orElseGet(() -> containsAny(source, "TRF1", "TRF2", "TRF3", "TRF4", "TRF5", "TRF6") ? source.replaceAll(".*(TRF[1-6]).*", "$1")
                        : containsAny(source, "TST", "STJ", "STF", "TSE", "STM") ? source.replaceAll(".*(TST|STJ|STF|TSE|STM).*", "$1")
                        : containsAny(source, "TRE") ? "TRE"
                        : containsAny(source, "TRT") ? "TRT"
                        : containsAny(source, "TJ") ? "TJ"
                        : null);
    }

    private static String inferRamo(String source, SecretariatFlowBridgeProfile bridgeProfile) {
        if (containsAny(source, "TRABALH", "TRT", "CLT")) {
            return "TRABALHISTA";
        }
        if (containsAny(source, "ELEITORAL", "TRE", "ZONA ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsAny(source, "MILITAR", "STM", "AUDITORIA")) {
            return "MILITAR";
        }
        if (containsAny(source, "PREVIDENCI", "TRF", "FEDERAL")) {
            return "PREVIDENCIARIO";
        }
        if (bridgeProfile != null && bridgeProfile.requiresRecursalSync()) {
            return "RECURSAL";
        }
        return "CIVEL";
    }

    private static String inferCompetencia(String source, SecretariatFlowBridgeProfile bridgeProfile) {
        if (containsAny(source, "TRABALH", "TRT")) {
            return "TRABALHO";
        }
        if (containsAny(source, "ELEITORAL", "TRE")) {
            return "ELEITORAL";
        }
        if (containsAny(source, "MILITAR", "STM", "AUDITORIA")) {
            return "MILITAR";
        }
        if (containsAny(source, "TRF", "FEDERAL", "PREVIDENCI")) {
            return "FEDERAL";
        }
        if (bridgeProfile != null && bridgeProfile.requiresRecursalSync()) {
            return "SUPERIOR";
        }
        return "ESTADUAL";
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
