package com.tcc.pjb.backend.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextService;
import com.tcc.pjb.backend.core.kernel.advisory.KernelAdvisoryTelemetry;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementIntelligenceService;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicyResolver;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicySnapshotReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsService;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationService;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationMessageDecision;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationReleaseGuard;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.dto.acordo.AcordoIntelligenceDto;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.intelligence.JudgeAgreementApprovalService;
import com.tcc.pjb.backend.service.intelligence.ProcessOutcomePredictionService;
import com.tcc.pjb.backend.service.intelligence.QualifiedThemeProactiveService;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcordoIntelligenceOrchestrator {

    private final ProcessoRepository processoRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;
    private final ChatMensagemRepository chatMensagemRepository;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final SettlementIntelligenceService settlementIntelligenceService;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final SettlementAdvisoryService settlementAdvisoryService;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;
    private final InstitutionalGovernanceContextService institutionalGovernanceContextService;
    private final NegotiationMemoryService negotiationMemoryService;
    private final NegotiationExplainabilityService negotiationExplainabilityService;
    private final NegotiationChatDigestService negotiationChatDigestService;
    private final NegotiationApprovalMatrixService negotiationApprovalMatrixService;
    private final NegotiationChannelGovernanceService negotiationChannelGovernanceService;
    private final KernelOperationalGovernanceService kernelOperationalGovernanceService;
    private final InstitutionalPolicyResolver institutionalPolicyResolver;
    private final KernelDecisionMetricsService kernelDecisionMetricsService;
    private final KernelRiskEscalationService kernelRiskEscalationService;
    private final NegotiationReleaseGuard negotiationReleaseGuard;
    private final ProcessOutcomePredictionService outcomePredictionService;
    private final QualifiedThemeProactiveService qualifiedThemeProactiveService;
    private final JudgeAgreementApprovalService judgeAgreementApprovalService;

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.kernel.advisory.NegotiationWindowReport analisarJanelaNegocial(Long processoId, BigDecimal valorPretendido) {
        Processo processo = buscarProcesso(processoId);
        List<String> baseSignals = buildNegotiationSignals(processo);
        ProcessMaterialDossierReport materialDossier = processMaterialDossierService.analyzeProcess(processo, baseSignals);
        ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeProcess(processo, materialDossier, baseSignals);
        return settlementIntelligenceService.analyze(
                processo,
                valorPretendido,
                mergeNegotiationSignals(processo, materialDossier, materialStrategy)
        );
    }

    @Transactional(readOnly = true)
    public SettlementAdvisoryReport analisarJanelaNegocialEstruturada(Long processoId, BigDecimal valorPretendido) {
        Processo processo = buscarProcesso(processoId);
        List<String> baseSignals = buildNegotiationSignals(processo);
        ProcessMaterialDossierReport materialDossier = processMaterialDossierService.analyzeProcess(processo, baseSignals);
        ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeProcess(processo, materialDossier, baseSignals);
        return settlementAdvisoryService.analyze(
                processo,
                processoRitoSnapshotService.resolve(processo, null).ritoCode(),
                valorPretendido,
                mergeNegotiationSignals(processo, materialDossier, materialStrategy),
                null
        );
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("DataFlowIssue")
    public AcordoIntelligenceDto analisarInteligenciaNegocial(Long processoId, BigDecimal valorPretendido) {
        Processo processo = buscarProcesso(processoId);
        String ritoName = processoRitoSnapshotService.resolve(processo, null).ritoCode();
        PropostaAcordo proposta = propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processoId).orElse(null);
        List<ChatMensagem> recentChat = recentChat(processoId);
        List<String> baseSignals = buildNegotiationSignals(processo);
        ProcessMaterialDossierReport materialDossier = processMaterialDossierService.analyzeProcess(processo, baseSignals);
        ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeProcess(processo, materialDossier, baseSignals);
        ProceduralRoutingReport proceduralRouting = nationalProceduralRoutingService.analyzeProcess(processo);
        ProceduralSubmissionBlueprintReport submissionBlueprint = proceduralSubmissionBlueprintService.analyzeProcess(processo, proceduralRouting);
        ProceduralConnectorExecutionReport connectorExecution = proceduralConnectorExecutionService.analyzeProcess(processo, proceduralRouting, submissionBlueprint);
        SettlementAdvisoryReport settlementAdvisory = settlementAdvisoryService.analyze(
                processo,
                ritoName,
                valorPretendido != null ? valorPretendido : proposta != null ? proposta.getValorAcordo() : null,
                mergeNegotiationSignals(processo, materialDossier, materialStrategy),
                null
        );
        InstitutionalGovernanceContextReport institutionalGovernanceContext = institutionalGovernanceContextService.analyzeProcess(
                processo,
                ritoName,
                settlementAdvisory,
                null,
                null
        );
        NegotiationMemoryReport negotiationMemory = negotiationMemoryService.analyzeProcess(
                processo,
                proposta,
                recentChat,
                settlementAdvisory,
                institutionalGovernanceContext
        );
        NegotiationExplainabilityReport negotiationExplainability = negotiationExplainabilityService.compose(
                processo,
                proposta,
                recentChat,
                settlementAdvisory,
                negotiationMemory,
                institutionalGovernanceContext
        );
        KernelOperationalGovernanceReport kernelOperationalGovernance = kernelOperationalGovernanceService.analyzeProcess(
                processo,
                ritoName,
                null,
                null,
                institutionalGovernanceContext,
                negotiationMemory,
                negotiationExplainability,
                null,
                null
        );
        NegotiationChatDigestReport negotiationChatDigest = negotiationChatDigestService.analyzeProcess(
                processo,
                proposta,
                recentChat,
                settlementAdvisory,
                negotiationMemory,
                negotiationExplainability,
                institutionalGovernanceContext,
                kernelOperationalGovernance
        );
        NegotiationApprovalMatrixReport negotiationApprovalMatrix = negotiationApprovalMatrixService.analyzeProcess(
                processo,
                proposta,
                recentChat,
                institutionalGovernanceContext,
                kernelOperationalGovernance,
                negotiationMemory,
                negotiationExplainability,
                negotiationChatDigest
        );
        NegotiationChannelGovernanceReport negotiationChannelGovernance = negotiationChannelGovernanceService.analyzeProcess(
                processo,
                proposta,
                recentChat,
                institutionalGovernanceContext,
                kernelOperationalGovernance,
                negotiationMemory,
                negotiationExplainability,
                negotiationChatDigest,
                negotiationApprovalMatrix
        );
        InstitutionalPolicySnapshotReport institutionalPolicySnapshot = institutionalPolicyResolver.resolve(
                processo,
                proposta,
                recentChat,
                institutionalGovernanceContext,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                ritoName
        );
        KernelDecisionMetricsReport kernelDecisionMetrics = kernelDecisionMetricsService.analyzeProcess(processo);
        KernelRiskEscalationReport kernelRiskEscalation = kernelRiskEscalationService.analyzeProcess(
                processo,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance
        );
        NegotiationMessageDecision governedMessageDecision = negotiationReleaseGuard.decide(
                negotiationChatDigest != null ? negotiationChatDigest.suggestedNextMessage() : null,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance
        );
        var outcomePrediction = outcomePredictionService.analyze(processo);
        var qualifiedThemes = qualifiedThemeProactiveService.analyze(processo);
        var judgeAgreementApprovalPrompt = judgeAgreementApprovalService.preview(processo, proposta, settlementAdvisory, outcomePrediction);
        KernelAdvisoryTelemetry advisoryTelemetry = kernelOperationalGovernanceService.buildTelemetry(
                "ACORDO_INTELLIGENCE",
                ritoName,
                settlementAdvisory,
                institutionalGovernanceContext,
                negotiationMemory,
                materialDossier,
                materialStrategy,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision
        );
        return new AcordoIntelligenceDto(
                processo.getId(),
                proposta != null ? proposta.getId() : null,
                processo.getNumeroUnificado(),
                ritoName,
                settlementAdvisory,
                institutionalGovernanceContext,
                negotiationMemory,
                materialDossier,
                materialStrategy,
                proceduralRouting,
                submissionBlueprint,
                connectorExecution,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision,
                advisoryTelemetry,
                outcomePrediction,
                qualifiedThemes,
                judgeAgreementApprovalPrompt,
                buildStrategicFocus(
                        settlementAdvisory,
                        institutionalGovernanceContext,
                        negotiationMemory,
                        materialDossier,
                        materialStrategy,
                        proceduralRouting,
                        submissionBlueprint,
                        connectorExecution,
                        negotiationExplainability,
                        kernelOperationalGovernance,
                        negotiationChatDigest,
                        negotiationApprovalMatrix,
                        negotiationChannelGovernance,
                        institutionalPolicySnapshot,
                        kernelDecisionMetrics,
                        kernelRiskEscalation,
                        governedMessageDecision
                )
        );
    }

    private Processo buscarProcesso(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", id));
    }

    private List<ChatMensagem> recentChat(Long processoId) {
        return chatMensagemRepository.findTop80ByProcesso_IdOrderByDataEnvioDesc(processoId).stream()
                .sorted(Comparator.comparing(ChatMensagem::getDataEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<String> buildNegotiationSignals(Processo processo) {
        List<String> signals = new java.util.ArrayList<>();
        if (processo == null) {
            return List.of();
        }
        if (processo.getFaseAtual() != null) {
            signals.add("Fase atual: " + processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            signals.add("Status do processo: " + processo.getStatusProcesso().name());
        }
        if (processo.getObjetoProcessual() != null && !processo.getObjetoProcessual().isBlank()) {
            signals.add("Objeto processual: " + processo.getObjetoProcessual().trim());
        }
        if (processo.getPedidoPrincipal() != null && !processo.getPedidoPrincipal().isBlank()) {
            signals.add("Pedido principal: " + processo.getPedidoPrincipal().trim());
        }
        if (processo.getMaterialProbatorioResumo() != null && !processo.getMaterialProbatorioResumo().isBlank()) {
            signals.add("Base probatória: " + compact(processo.getMaterialProbatorioResumo(), 220));
        }
        if (processo.getJanelaAcordoResumo() != null && !processo.getJanelaAcordoResumo().isBlank()) {
            signals.add(processo.getJanelaAcordoResumo().trim());
        }
        if (processo.getPotencialAcordoScore() != null) {
            signals.add("Potencial negocial: " + processo.getPotencialAcordoScore() + "/100");
        }
        if (processo.getResultadoFinal() != null && !processo.getResultadoFinal().isBlank()) {
            signals.add(processo.getResultadoFinal().trim());
        }
        return List.copyOf(signals);
    }

    private List<String> mergeNegotiationSignals(Processo processo, ProcessMaterialDossierReport materialDossier, ProcessMaterialStrategyReport materialStrategy) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(buildNegotiationSignals(processo));
        if (materialDossier != null) {
            merged.addAll(materialDossier.evidenceAnchors());
            merged.addAll(materialDossier.settlementLevers());
            merged.addAll(materialDossier.proofGaps());
        }
        if (materialStrategy != null) {
            merged.add(materialStrategy.litigationPosture());
            merged.add(materialStrategy.protocolReadiness());
            merged.add(materialStrategy.negotiationStance());
            merged.addAll(materialStrategy.protocolBlockers());
            merged.addAll(materialStrategy.negotiationGuardrails());
            merged.addAll(materialStrategy.controlPoints());
        }
        merged.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(merged);
    }

    private List<String> buildStrategicFocus(SettlementAdvisoryReport settlementAdvisory,
                                             InstitutionalGovernanceContextReport institutionalGovernanceContext,
                                             NegotiationMemoryReport negotiationMemory,
                                             ProcessMaterialDossierReport materialDossier,
                                             ProcessMaterialStrategyReport materialStrategy,
                                             ProceduralRoutingReport proceduralRouting,
                                             ProceduralSubmissionBlueprintReport submissionBlueprint,
                                             ProceduralConnectorExecutionReport connectorExecution,
                                             NegotiationExplainabilityReport negotiationExplainability,
                                             KernelOperationalGovernanceReport kernelOperationalGovernance,
                                             NegotiationChatDigestReport negotiationChatDigest,
                                             NegotiationApprovalMatrixReport negotiationApprovalMatrix,
                                             NegotiationChannelGovernanceReport negotiationChannelGovernance,
                                             InstitutionalPolicySnapshotReport institutionalPolicySnapshot,
                                             KernelDecisionMetricsReport kernelDecisionMetrics,
                                             KernelRiskEscalationReport kernelRiskEscalation,
                                             NegotiationMessageDecision governedMessageDecision) {
        LinkedHashSet<String> focus = new LinkedHashSet<>();
        if (settlementAdvisory != null) {
            focus.addAll(settlementAdvisory.nextMoves());
            focus.addAll(settlementAdvisory.executionSafeguards());
        }
        if (institutionalGovernanceContext != null) {
            focus.addAll(institutionalGovernanceContext.policyGuards());
            focus.addAll(institutionalGovernanceContext.escalationPlaybooks());
        }
        if (negotiationMemory != null) {
            focus.addAll(negotiationMemory.reusablePlaybooks());
            focus.addAll(negotiationMemory.cautionPoints());
        }
        if (materialDossier != null) {
            focus.add(materialDossier.objectLabel());
            focus.add(materialDossier.primaryRelief());
            focus.add(materialDossier.evidentiaryBracket());
            focus.add(materialDossier.negotiationBracket());
            focus.addAll(materialDossier.controversyAxes());
            focus.addAll(materialDossier.thesisVectors());
            focus.addAll(materialDossier.evidenceAnchors());
            focus.addAll(materialDossier.proofGaps());
            focus.addAll(materialDossier.settlementLevers());
            focus.addAll(materialDossier.protocolChecklist());
        }
        if (materialStrategy != null) {
            focus.add(materialStrategy.litigationPosture());
            focus.add(materialStrategy.protocolReadiness());
            focus.add(materialStrategy.negotiationStance());
            focus.add(materialStrategy.evidenceReadiness());
            focus.addAll(materialStrategy.pleadingBlueprint());
            focus.addAll(materialStrategy.evidenceAgenda());
            focus.addAll(materialStrategy.protocolBlockers());
            focus.addAll(materialStrategy.negotiationGuardrails());
            focus.addAll(materialStrategy.executionChecklist());
            focus.addAll(materialStrategy.controlPoints());
        }
        if (proceduralRouting != null) {
            focus.add(proceduralRouting.tipoJusticaSugerida());
            focus.add(proceduralRouting.ritoSugerido());
            focus.add(proceduralRouting.proceduralRegime());
            focus.add(proceduralRouting.proceduralTrack());
            focus.add(proceduralRouting.actionNature());
            focus.add(proceduralRouting.probatoryProfile());
            focus.add(proceduralRouting.complexityBand());
            focus.add(proceduralRouting.tribunalCodigo());
            focus.add(proceduralRouting.varaSugerida());
            focus.addAll(proceduralRouting.blockingIssues());
            focus.addAll(proceduralRouting.reasons());
            focus.addAll(proceduralRouting.alerts());
            focus.addAll(proceduralRouting.reviewChecklist());
            if (proceduralRouting.economicGate() != null) {
                focus.add(proceduralRouting.economicGate().economicBand());
                focus.add(proceduralRouting.economicGate().thresholdKind());
                focus.addAll(proceduralRouting.economicGate().reasons());
                focus.addAll(proceduralRouting.economicGate().rerouteOptions());
                focus.addAll(proceduralRouting.economicGate().reviewChecklist());
            }
        }
        if (submissionBlueprint != null) {
            focus.add(submissionBlueprint.blueprintStatus());
            focus.add(submissionBlueprint.localCorrelationMode());
            focus.add(submissionBlueprint.tribunalCodigo());
            focus.add(submissionBlueprint.unidadeJudiciariaCodigo());
            focus.add(submissionBlueprint.judicialSystem() != null ? submissionBlueprint.judicialSystem().name() : null);
            focus.add(submissionBlueprint.dryRunStatus());
            focus.addAll(submissionBlueprint.relatedLocalProcessNumbers());
            focus.addAll(submissionBlueprint.blockingIssues());
            focus.addAll(submissionBlueprint.reviewChecklist());
            focus.addAll(submissionBlueprint.warnings());
        }
        if (connectorExecution != null) {
            focus.add(connectorExecution.executionMode());
            focus.add(connectorExecution.submissionLane());
            focus.add(connectorExecution.tribunalTargetKey());
            focus.add(connectorExecution.protocolClassCode());
            focus.add(connectorExecution.signerMode());
            focus.add(connectorExecution.retryPolicy());
            focus.addAll(connectorExecution.phases());
            focus.addAll(connectorExecution.executionChecklist());
            focus.addAll(connectorExecution.blockers());
            focus.addAll(connectorExecution.warnings());
        }
        if (negotiationExplainability != null) {
            focus.addAll(negotiationExplainability.openQuestions());
            negotiationExplainability.nodes().stream().map(NegotiationExplainabilityReport.NegotiationNode::title).forEach(focus::add);
        }
        if (kernelOperationalGovernance != null) {
            focus.addAll(kernelOperationalGovernance.nextActions());
            focus.addAll(kernelOperationalGovernance.watchpoints());
        }
        if (negotiationChatDigest != null) {
            focus.addAll(negotiationChatDigest.anchorNarratives());
            focus.addAll(negotiationChatDigest.protectedTopics());
            focus.addAll(negotiationChatDigest.escalationSignals());
            focus.addAll(negotiationChatDigest.nextTurnObjectives());
            focus.addAll(negotiationChatDigest.forbiddenMoves());
            focus.addAll(negotiationChatDigest.internalActions());
            focus.addAll(negotiationChatDigest.messageBlueprints());
        }
        if (negotiationApprovalMatrix != null) {
            focus.addAll(negotiationApprovalMatrix.approvalGates());
            focus.addAll(negotiationApprovalMatrix.escalationLanes());
            focus.addAll(negotiationApprovalMatrix.internalControls());
            focus.addAll(negotiationApprovalMatrix.releaseChecklist());
        }
        if (negotiationChannelGovernance != null) {
            focus.add(negotiationChannelGovernance.operatingMode());
            focus.add(negotiationChannelGovernance.persistenceMode());
            focus.add(negotiationChannelGovernance.approvalHandshake());
            focus.addAll(negotiationChannelGovernance.participantDirectives());
            focus.addAll(negotiationChannelGovernance.releaseBoundaries());
            focus.addAll(negotiationChannelGovernance.auditDirectives());
            focus.addAll(negotiationChannelGovernance.memoryDirectives());
            focus.addAll(negotiationChannelGovernance.deliveryGuardrails());
            focus.addAll(negotiationChannelGovernance.fallbackLanes());
        }
        if (institutionalPolicySnapshot != null) {
            focus.add(institutionalPolicySnapshot.policyKey());
            focus.add(institutionalPolicySnapshot.policyTier());
            if (institutionalPolicySnapshot.policyAxes() != null) {
                focus.add(institutionalPolicySnapshot.policyAxes().selectionMode());
                focus.addAll(institutionalPolicySnapshot.policyAxes().matchedAxes());
                focus.addAll(institutionalPolicySnapshot.policyAxes().declaredAxes());
            }
            focus.addAll(institutionalPolicySnapshot.mandatoryDirectives());
            focus.addAll(institutionalPolicySnapshot.blockingDirectives());
            focus.addAll(institutionalPolicySnapshot.releaseGuardrails());
            focus.addAll(institutionalPolicySnapshot.escalationTriggers());
        }
        if (kernelDecisionMetrics != null) {
            focus.addAll(kernelDecisionMetrics.hotSignals());
            focus.addAll(kernelDecisionMetrics.stabilitySignals());
        }
        if (kernelRiskEscalation != null) {
            focus.add(kernelRiskEscalation.escalationLevel());
            focus.addAll(kernelRiskEscalation.containmentActions());
            focus.addAll(kernelRiskEscalation.escalationTriggers());
            focus.addAll(kernelRiskEscalation.recommendedLanes());
        }
        if (governedMessageDecision != null) {
            focus.add(governedMessageDecision.decisionCode());
            focus.addAll(governedMessageDecision.reasons());
            focus.addAll(governedMessageDecision.mandatoryActions());
            focus.add(governedMessageDecision.releaseMessage());
        }
        focus.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(focus);
    }

    private static String compact(String value, int max) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, max));
    }
}
