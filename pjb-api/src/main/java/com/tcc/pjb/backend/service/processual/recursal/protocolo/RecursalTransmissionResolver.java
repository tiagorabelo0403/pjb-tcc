package com.tcc.pjb.backend.service.processual.recursal.protocolo;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorContingencyProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorContingencyResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorReplayProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorReplayResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSubmissionAuditProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSubmissionAuditResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSystemProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorSystemResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorTelemetryProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorTelemetryResolver;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorWorkflowProfile;
import com.tcc.pjb.backend.integration.judicial.routing.JudicialConnectorWorkflowResolver;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityProfile;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RecursalTransmissionResolver {

    private final RecursalProtocolGovernanceResolver protocolGovernanceResolver;
    private final JudicialConnectorWorkflowResolver connectorWorkflowResolver;
    private final JudicialConnectorContingencyResolver connectorContingencyResolver;
    private final JudicialConnectorSystemResolver connectorSystemResolver;
    private final JudicialConnectorReplayResolver connectorReplayResolver;
    private final JudicialConnectorTelemetryResolver connectorTelemetryResolver;
    private final JudicialConnectorSubmissionAuditResolver submissionAuditResolver;

    public RecursalTransmissionResolver(RecursalProtocolGovernanceResolver protocolGovernanceResolver,
                                        JudicialConnectorWorkflowResolver connectorWorkflowResolver,
                                        JudicialConnectorContingencyResolver connectorContingencyResolver,
                                        JudicialConnectorSystemResolver connectorSystemResolver,
                                        JudicialConnectorReplayResolver connectorReplayResolver,
                                        JudicialConnectorTelemetryResolver connectorTelemetryResolver,
                                        JudicialConnectorSubmissionAuditResolver submissionAuditResolver) {
        this.protocolGovernanceResolver = protocolGovernanceResolver;
        this.connectorWorkflowResolver = connectorWorkflowResolver;
        this.connectorContingencyResolver = connectorContingencyResolver;
        this.connectorSystemResolver = connectorSystemResolver;
        this.connectorReplayResolver = connectorReplayResolver;
        this.connectorTelemetryResolver = connectorTelemetryResolver;
        this.submissionAuditResolver = submissionAuditResolver;
    }

    public RecursalTransmissionProfile resolve(RecursalAdmissibilityService.RecursalAdmissibilityCommand command,
                                               RecursalPlanningResult planning,
                                               RecursalAdmissibilityProfile admissibilityProfile) {
        var plan = planning.routePlan();
        String originCourt = plan.tribunalOrigem().name();
        String destinationCourt = plan.tribunalDestino().name();
        boolean remessaExterna = plan.remessa().externa();
        boolean autuacaoDestino = plan.remessa().autuacaoDestino();
        boolean distribuicaoDestino = plan.remessa().distribuicaoDestino();
        boolean preven = plan.prevencao().obrigatoria();
        boolean urgente = command.pedidoEfeitoSuspensivo() || command.tutelaUrgenciaRecursal() || command.priorizaIdosoOuSaude();

        Map<String, Object> routingPayload = new LinkedHashMap<>();
        routingPayload.put("tribunalCodigo", destinationCourt);
        routingPayload.put("tribunal", destinationCourt);
        routingPayload.put("uf", command.uf());
        routingPayload.put("comarca", command.comarca());
        JudicialConnectorWorkflowProfile workflowProfile = connectorWorkflowResolver.resolve(
                routingPayload,
                command.planRequest().context().rito().name(),
                command.planRequest().context().ramo().name(),
                command.planRequest().context().tipoJustica().name(),
                true,
                command.segredoJustica(),
                urgente
        );
        JudicialConnectorContingencyProfile contingencyProfile = connectorContingencyResolver.resolve(
                workflowProfile.connectorSystem(),
                workflowProfile,
                command.segredoJustica(),
                urgente,
                true
        );
        JudicialConnectorSystemProfile systemProfile = connectorSystemResolver.resolve(
                workflowProfile.connectorSystem(),
                destinationCourt,
                workflowProfile.competenceHint(),
                command.segredoJustica(),
                urgente,
                true
        );
        JudicialConnectorReplayProfile replayProfile = connectorReplayResolver.resolve(
                systemProfile,
                workflowProfile,
                contingencyProfile,
                command.segredoJustica(),
                urgente,
                true
        );
        JudicialConnectorTelemetryProfile telemetryProfile = connectorTelemetryResolver.resolve(
                systemProfile,
                workflowProfile,
                contingencyProfile,
                replayProfile,
                command.segredoJustica(),
                urgente,
                true
        );
        JudicialConnectorSubmissionAuditProfile submissionAuditProfile = submissionAuditResolver.resolve(
                systemProfile,
                workflowProfile,
                contingencyProfile,
                replayProfile,
                telemetryProfile,
                command.segredoJustica(),
                urgente,
                true
        );

        String protocolDesk = firstNonBlank(workflowProfile.protocolDesk(), systemProfile.protocolNamespace(), "PROTOCOLO_RECURSAL_" + originCourt);
        String remessaDesk = remessaExterna ? "REMESSA_INTERTRIBUNAL_" + destinationCourt : "REMESSA_INTERNA_" + destinationCourt;
        String autuacaoDesk = autuacaoDestino ? "AUTUACAO_RECURSAL_" + destinationCourt : null;
        String integrationChannel = remessaExterna ? firstNonBlank(workflowProfile.connectorSystem(), connectorFor(destinationCourt)) : "PJB_INTERNAL_REC";
        String credentialMode = workflowProfile.stepUpRequired() || workflowProfile.certificateRequired()
                ? "CREDENCIAL_ASSISTIDA"
                : command.segredoJustica() ? "CREDENCIAL_REFORCADA"
                : urgente ? "CREDENCIAL_PRIORITARIA"
                : "CREDENCIAL_PADRAO";
        String payloadPolicy = command.segredoJustica() ? "PAYLOAD_MINIMIZADO"
                : remessaExterna ? "PACOTE_CERTIFICADO"
                : systemProfile.evidenceStore() != null && systemProfile.evidenceStore().contains("DOSSIE") ? "PACOTE_AUDITAVEL"
                : "PACOTE_INTERNO";
        String transmissionMode = switch (plan.routeKind()) {
            case INTERNAL_SAME_AUTOS -> preven ? "TRAMITE_INTERNO_PREVENTO" : "TRAMITE_INTERNO_MESMOS_AUTOS";
            case INTERNAL_REGIMENTAL -> "TRAMITE_INTERNO_REGIMENTAL";
            case EXECUTION_INCIDENT_INTERNAL -> "INCIDENTE_EXECUTIVO_MESMOS_AUTOS";
            case JUIZADO_TURMA_RECURSAL -> autuacaoDestino ? "REMESSA_TURMA_RECURSAL_AUTUACAO" : "REMESSA_TURMA_RECURSAL_MESMA_NUMERACAO";
            case JUIZADO_UNIFORMIZACAO -> autuacaoDestino ? "REMESSA_UNIFORMIZACAO_AUTUACAO" : "REMESSA_UNIFORMIZACAO_MESMA_NUMERACAO";
            case SUPERIOR_EXCEPTIONAL -> autuacaoDestino ? "REMESSA_SUPERIOR_ADMISSIBILIDADE" : "REMESSA_SUPERIOR_MESMA_NUMERACAO";
            case EXTRAORDINARY_EXCEPTIONAL -> autuacaoDestino ? "REMESSA_EXTRAORDINARIA_ADMISSIBILIDADE" : "REMESSA_EXTRAORDINARIA_MESMA_NUMERACAO";
            case ORIGINARY_SUPERIOR -> "AUTUACAO_ORIGINARIA_SUPERIOR";
            case ORIGINARY_CONSTITUTIONAL -> "AUTUACAO_ORIGINARIA_CONSTITUCIONAL";
            case SECOND_INSTANCE_EXTERNAL -> autuacaoDestino ? "REMESSA_SEGUNDO_GRAU_AUTUACAO"
                    : distribuicaoDestino ? "REMESSA_SEGUNDO_GRAU_MESMA_NUMERACAO"
                    : remessaExterna && workflowProfile.stepUpRequired() ? "REMESSA_SEGUNDO_GRAU_ASSISTIDA"
                    : remessaExterna ? "REMESSA_SEGUNDO_GRAU_DIRETA"
                    : "TRAMITE_INTERNO";
        };
        String queueSuffix = (originCourt + '_' + destinationCourt + '_' + plan.instanciaDestino().name() + '_' + transmissionMode).replace(' ', '_');
        String reviewDesk = urgente
                ? "REVISAO_PRIORITARIA_RECURSAL_" + destinationCourt
                : command.segredoJustica() ? "REVISAO_SIGILO_RECURSAL_" + destinationCourt
                : admissibilityProfile.supportDesk();

        RecursalProtocolGovernanceProfile governance = protocolGovernanceResolver.resolve(command, planning, admissibilityProfile, destinationCourt);

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(transmissionMode);
        labels.add(integrationChannel);
        labels.add(payloadPolicy);
        if (command.segredoJustica()) {
            labels.add("SEGREDO_JUSTICA");
        }
        if (command.pedidoEfeitoSuspensivo()) {
            labels.add("EFEITO_SUSPENSIVO");
        }
        if (command.tutelaUrgenciaRecursal()) {
            labels.add("URGENTE_RECURSAL");
        }
        if (command.priorizaIdosoOuSaude()) {
            labels.add("PRIORIDADE_LEGAL");
        }
        if (distribuicaoDestino) {
            labels.add("DISTRIBUICAO_DESTINO");
        }
        labels.addAll(contingencyProfile.labels());
        labels.addAll(replayProfile.labels());
        labels.addAll(telemetryProfile.labels());
        labels.addAll(submissionAuditProfile.labels());
        labels.addAll(governance.labels());
        labels.addAll(systemProfile.labels());
        labels.addAll(workflowProfile.labels());

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("originCourt", originCourt);
        metadata.put("destinationCourt", destinationCourt);
        metadata.put("remessaExterna", remessaExterna);
        metadata.put("autuacaoDestino", autuacaoDestino);
        metadata.put("distribuicaoDestino", distribuicaoDestino);
        metadata.put("preventionRequired", preven);
        metadata.put("routeKind", plan.routeKind().name());
        metadata.put("routeDescriptor", plan.routeKind().descriptor());
        metadata.putAll(governance.toMap());
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
        metadata.put("descriptor", protocolDesk + ':' + remessaDesk + ':' + transmissionMode);

        return new RecursalTransmissionProfile(
                protocolDesk,
                remessaDesk,
                autuacaoDesk,
                integrationChannel,
                credentialMode,
                payloadPolicy,
                transmissionMode,
                queueSuffix,
                reviewDesk,
                firstNonBlank(governance.ackDesk(), telemetryProfile.telemetryChannel(), replayProfile.replayEvidenceDesk(), systemProfile.protocolNamespace() == null ? null : systemProfile.protocolNamespace() + "_ACK"),
                firstNonBlank(workflowProfile.receiptChannel(), governance.receiptChannel(), replayProfile.receiptCorrelationMode(), systemProfile.receiptPattern()),
                firstNonBlank(governance.retryMode(), replayProfile.replayMode(), telemetryProfile.deadLetterQueue(), systemProfile.replayStrategy()),
                firstNonBlank(workflowProfile.evidenceEnvelope(), governance.evidencePolicy(), telemetryProfile.observabilityPolicy(), replayProfile.deliveryAssuranceMode(), systemProfile.evidenceStore()),
                firstNonBlank(governance.complianceDesk(), telemetryProfile.reconciliationDesk()),
                firstNonBlank(workflowProfile.submissionWindow(), governance.protocolWindow()),
                workflowProfile.connectorSystem(),
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
                workflowProfile.competenceHint(),
                workflowProfile.stepUpRequired(),
                workflowProfile.certificateRequired(),
                workflowProfile.warnings(),
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

    private static String connectorFor(String destinationCourt) {
        if (destinationCourt == null || destinationCourt.isBlank()) {
            return "PJB_REC_DEFAULT";
        }
        return switch (destinationCourt) {
            case "STJ", "STF", "TST", "STM", "TSE" -> destinationCourt + "_GOV_BRIDGE";
            default -> destinationCourt.startsWith("TRF") || destinationCourt.startsWith("TRT") || destinationCourt.startsWith("TRE")
                    ? destinationCourt + "_TRIBUNAL_BRIDGE"
                    : destinationCourt + "_COURT_BRIDGE";
        };
    }
}
