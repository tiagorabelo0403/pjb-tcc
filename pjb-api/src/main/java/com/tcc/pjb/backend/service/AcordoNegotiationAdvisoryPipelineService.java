package com.tcc.pjb.backend.service;

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
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicyResolver;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicySnapshotReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsService;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationService;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationMessageDecision;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationReleaseGuard;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Encadeia os 12 colaboradores de "advisory" de negociação (settlement, governança
 * institucional, memória, explicabilidade, digest de chat, matriz de aprovação, governança de
 * canal, política institucional, métricas de decisão, escalonamento de risco e o guard de
 * liberação de mensagem) num único relatório consolidado. Extraído de
 * {@link AcordoSuggestionPipelineAsyncService} porque esses colaboradores são usados
 * exclusivamente por esse pipeline -- nenhum é tocado por loadPipelineSeed, persistPipelineOutcome
 * ou pela geração da minuta via IA.
 */
@Service
public class AcordoNegotiationAdvisoryPipelineService {

    private final SettlementAdvisoryService settlementAdvisoryService;
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

    public AcordoNegotiationAdvisoryPipelineService(SettlementAdvisoryService settlementAdvisoryService,
                                                     InstitutionalGovernanceContextService institutionalGovernanceContextService,
                                                     NegotiationMemoryService negotiationMemoryService,
                                                     NegotiationExplainabilityService negotiationExplainabilityService,
                                                     NegotiationChatDigestService negotiationChatDigestService,
                                                     NegotiationApprovalMatrixService negotiationApprovalMatrixService,
                                                     NegotiationChannelGovernanceService negotiationChannelGovernanceService,
                                                     KernelOperationalGovernanceService kernelOperationalGovernanceService,
                                                     InstitutionalPolicyResolver institutionalPolicyResolver,
                                                     KernelDecisionMetricsService kernelDecisionMetricsService,
                                                     KernelRiskEscalationService kernelRiskEscalationService,
                                                     NegotiationReleaseGuard negotiationReleaseGuard) {
        this.settlementAdvisoryService = Objects.requireNonNull(settlementAdvisoryService, "settlementAdvisoryService");
        this.institutionalGovernanceContextService = Objects.requireNonNull(institutionalGovernanceContextService, "institutionalGovernanceContextService");
        this.negotiationMemoryService = Objects.requireNonNull(negotiationMemoryService, "negotiationMemoryService");
        this.negotiationExplainabilityService = Objects.requireNonNull(negotiationExplainabilityService, "negotiationExplainabilityService");
        this.negotiationChatDigestService = Objects.requireNonNull(negotiationChatDigestService, "negotiationChatDigestService");
        this.negotiationApprovalMatrixService = Objects.requireNonNull(negotiationApprovalMatrixService, "negotiationApprovalMatrixService");
        this.negotiationChannelGovernanceService = Objects.requireNonNull(negotiationChannelGovernanceService, "negotiationChannelGovernanceService");
        this.kernelOperationalGovernanceService = Objects.requireNonNull(kernelOperationalGovernanceService, "kernelOperationalGovernanceService");
        this.institutionalPolicyResolver = Objects.requireNonNull(institutionalPolicyResolver, "institutionalPolicyResolver");
        this.kernelDecisionMetricsService = Objects.requireNonNull(kernelDecisionMetricsService, "kernelDecisionMetricsService");
        this.kernelRiskEscalationService = Objects.requireNonNull(kernelRiskEscalationService, "kernelRiskEscalationService");
        this.negotiationReleaseGuard = Objects.requireNonNull(negotiationReleaseGuard, "negotiationReleaseGuard");
    }

    public AcordoPipelineAnalysis analyze(Processo processo, PropostaAcordo proposta, String ritoName, List<ChatMensagem> recentChat) {
        SettlementAdvisoryReport settlementAdvisory = settlementAdvisoryService.analyze(
                processo,
                ritoName,
                proposta.getValorAcordo(),
                buildNegotiationSignals(processo),
                null
        );
        InstitutionalGovernanceContextReport governance = institutionalGovernanceContextService.analyzeProcess(processo, ritoName, settlementAdvisory, null, null);
        NegotiationMemoryReport negotiationMemory = negotiationMemoryService.analyzeProcess(processo, proposta, recentChat, settlementAdvisory, governance);
        NegotiationExplainabilityReport negotiationExplainability = negotiationExplainabilityService.compose(processo, proposta, recentChat, settlementAdvisory, negotiationMemory, governance);
        KernelOperationalGovernanceReport kernelOperationalGovernance = kernelOperationalGovernanceService.analyzeProcess(processo, ritoName, null, null, governance, negotiationMemory, negotiationExplainability, null, null);
        NegotiationChatDigestReport negotiationChatDigest = negotiationChatDigestService.analyzeProcess(processo, proposta, recentChat, settlementAdvisory, negotiationMemory, negotiationExplainability, governance, kernelOperationalGovernance);
        NegotiationApprovalMatrixReport negotiationApprovalMatrix = negotiationApprovalMatrixService.analyzeProcess(processo, proposta, recentChat, governance, kernelOperationalGovernance, negotiationMemory, negotiationExplainability, negotiationChatDigest);
        NegotiationChannelGovernanceReport negotiationChannelGovernance = negotiationChannelGovernanceService.analyzeProcess(processo, proposta, recentChat, governance, kernelOperationalGovernance, negotiationMemory, negotiationExplainability, negotiationChatDigest, negotiationApprovalMatrix);
        InstitutionalPolicySnapshotReport institutionalPolicySnapshot = institutionalPolicyResolver.resolve(processo, proposta, recentChat, governance, negotiationChatDigest, negotiationApprovalMatrix, negotiationChannelGovernance, ritoName);
        KernelDecisionMetricsReport kernelDecisionMetrics = kernelDecisionMetricsService.analyzeProcess(processo);
        KernelRiskEscalationReport kernelRiskEscalation = kernelRiskEscalationService.analyzeProcess(processo, institutionalPolicySnapshot, kernelDecisionMetrics, negotiationChatDigest, negotiationApprovalMatrix, negotiationChannelGovernance);
        NegotiationMessageDecision governedMessageDecision = negotiationReleaseGuard.decide(
                negotiationChatDigest != null ? negotiationChatDigest.suggestedNextMessage() : null,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance
        );
        KernelAdvisoryTelemetry telemetry = kernelOperationalGovernanceService.buildTelemetry(
                "ACORDO_PIPELINE",
                ritoName,
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision
        );
        List<String> strategicFocus = buildStrategicFocus(
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision
        );
        String intelligenceDigest = buildIntelligenceDigest(
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision,
                telemetry
        );
        String suggestedNextMessage = negotiationChatDigest != null ? negotiationChatDigest.suggestedNextMessage() : null;
        return new AcordoPipelineAnalysis(
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision,
                telemetry,
                strategicFocus,
                intelligenceDigest,
                suggestedNextMessage
        );
    }

    private String buildIntelligenceDigest(SettlementAdvisoryReport settlementAdvisory,
                                           InstitutionalGovernanceContextReport governance,
                                           NegotiationMemoryReport negotiationMemory,
                                           NegotiationExplainabilityReport negotiationExplainability,
                                           KernelOperationalGovernanceReport kernelOperationalGovernance,
                                           NegotiationChatDigestReport negotiationChatDigest,
                                           NegotiationApprovalMatrixReport negotiationApprovalMatrix,
                                           NegotiationChannelGovernanceReport negotiationChannelGovernance,
                                           InstitutionalPolicySnapshotReport institutionalPolicySnapshot,
                                           KernelDecisionMetricsReport kernelDecisionMetrics,
                                           KernelRiskEscalationReport kernelRiskEscalation,
                                           NegotiationMessageDecision governedMessageDecision,
                                           KernelAdvisoryTelemetry telemetry) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        lines.add("Snapshot de inteligência negocial atualizado.");
        if (settlementAdvisory != null) {
            lines.addAll(limit(settlementAdvisory.nextMoves(), 2));
            lines.addAll(limit(settlementAdvisory.executionSafeguards(), 2));
        }
        if (governance != null) {
            lines.addAll(limit(governance.policyGuards(), 2));
            lines.addAll(limit(governance.governanceAlerts(), 2));
        }
        if (negotiationMemory != null) {
            lines.addAll(limit(negotiationMemory.reusablePlaybooks(), 2));
            lines.addAll(limit(negotiationMemory.cautionPoints(), 2));
        }
        if (negotiationExplainability != null) {
            lines.addAll(limit(negotiationExplainability.openQuestions(), 2));
        }
        if (kernelOperationalGovernance != null) {
            lines.addAll(limit(kernelOperationalGovernance.nextActions(), 2));
            lines.addAll(limit(kernelOperationalGovernance.watchpoints(), 2));
        }
        if (negotiationChatDigest != null) {
            lines.add("Chat: " + negotiationChatDigest.conversationStage() + " | postura=" + negotiationChatDigest.posture() + " | temperatura=" + negotiationChatDigest.counterpartyTemperature() + " | envio=" + negotiationChatDigest.sendMode());
            lines.addAll(limit(negotiationChatDigest.anchorNarratives(), 2));
            lines.addAll(limit(negotiationChatDigest.protectedTopics(), 2));
            lines.addAll(limit(negotiationChatDigest.escalationSignals(), 2));
            lines.addAll(limit(negotiationChatDigest.nextTurnObjectives(), 2));
            lines.addAll(limit(negotiationChatDigest.forbiddenMoves(), 1));
            lines.addAll(limit(negotiationChatDigest.internalActions(), 2));
            lines.addAll(limit(negotiationChatDigest.messageBlueprints(), 1));
            if (negotiationChatDigest.suggestedNextMessage() != null && !negotiationChatDigest.suggestedNextMessage().isBlank()) {
                lines.add(negotiationChatDigest.suggestedNextMessage());
            }
        }
        if (negotiationApprovalMatrix != null) {
            lines.add("Approval: " + negotiationApprovalMatrix.approvalBand() + " | release=" + negotiationApprovalMatrix.releaseMode());
            lines.addAll(limit(negotiationApprovalMatrix.approvalGates(), 2));
            lines.addAll(limit(negotiationApprovalMatrix.escalationLanes(), 2));
            lines.addAll(limit(negotiationApprovalMatrix.internalControls(), 2));
            lines.addAll(limit(negotiationApprovalMatrix.releaseChecklist(), 2));
        }
        if (negotiationChannelGovernance != null) {
            lines.add("Canal: " + negotiationChannelGovernance.operatingMode() + " | persist=" + negotiationChannelGovernance.persistenceMode() + " | handshake=" + negotiationChannelGovernance.approvalHandshake());
            lines.addAll(limit(negotiationChannelGovernance.participantDirectives(), 2));
            lines.addAll(limit(negotiationChannelGovernance.releaseBoundaries(), 2));
            lines.addAll(limit(negotiationChannelGovernance.auditDirectives(), 2));
            lines.addAll(limit(negotiationChannelGovernance.memoryDirectives(), 2));
            lines.addAll(limit(negotiationChannelGovernance.deliveryGuardrails(), 2));
            lines.addAll(limit(negotiationChannelGovernance.fallbackLanes(), 2));
        }
        if (institutionalPolicySnapshot != null) {
            lines.add("Policy: " + institutionalPolicySnapshot.policyTier() + " | key=" + institutionalPolicySnapshot.policyKey() + " | strict=" + institutionalPolicySnapshot.strictRelease());
            if (institutionalPolicySnapshot.policyAxes() != null) {
                lines.add("Axes: mode=" + institutionalPolicySnapshot.policyAxes().selectionMode()
                        + " | ramo=" + institutionalPolicySnapshot.policyAxes().ramoDireito()
                        + " | materia=" + institutionalPolicySnapshot.policyAxes().materia()
                        + " | rito=" + institutionalPolicySnapshot.policyAxes().ritoProcessual());
                lines.addAll(limit(institutionalPolicySnapshot.policyAxes().matchedAxes(), 3));
            }
            lines.addAll(limit(institutionalPolicySnapshot.mandatoryDirectives(), 2));
            lines.addAll(limit(institutionalPolicySnapshot.blockingDirectives(), 2));
            lines.addAll(limit(institutionalPolicySnapshot.releaseGuardrails(), 2));
        }
        if (kernelDecisionMetrics != null) {
            lines.add("Metrics: total=" + kernelDecisionMetrics.totalDecisions() + " | blocked=" + kernelDecisionMetrics.blockedDecisions() + " | approval=" + kernelDecisionMetrics.approvalRequiredDecisions());
            lines.addAll(limit(kernelDecisionMetrics.hotSignals(), 2));
            lines.addAll(limit(kernelDecisionMetrics.stabilitySignals(), 2));
        }
        if (kernelRiskEscalation != null) {
            lines.add("Risk: level=" + kernelRiskEscalation.escalationLevel());
            lines.addAll(limit(kernelRiskEscalation.containmentActions(), 2));
            lines.addAll(limit(kernelRiskEscalation.recommendedLanes(), 2));
        }
        if (governedMessageDecision != null) {
            lines.add("Release: code=" + governedMessageDecision.decisionCode() + " | allowed=" + governedMessageDecision.releaseAllowed() + " | risk=" + governedMessageDecision.riskLevel());
            lines.addAll(limit(governedMessageDecision.reasons(), 2));
            lines.addAll(limit(governedMessageDecision.mandatoryActions(), 2));
        }
        if (telemetry != null) {
            lines.add("Telemetria: " + telemetry.statusBand() + " | componentes=" + telemetry.advisoryCount() + " | bloqueios=" + telemetry.blockingCount());
        }
        return String.join(" | ", lines);
    }

    private List<String> buildStrategicFocus(SettlementAdvisoryReport settlementAdvisory,
                                           InstitutionalGovernanceContextReport governance,
                                           NegotiationMemoryReport negotiationMemory,
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
        if (settlementAdvisory != null) { focus.addAll(settlementAdvisory.nextMoves()); focus.addAll(settlementAdvisory.executionSafeguards()); }
        if (governance != null) { focus.addAll(governance.policyGuards()); focus.addAll(governance.governanceAlerts()); }
        if (negotiationMemory != null) { focus.addAll(negotiationMemory.reusablePlaybooks()); focus.addAll(negotiationMemory.cautionPoints()); }
        if (negotiationExplainability != null) { focus.addAll(negotiationExplainability.openQuestions()); }
        if (kernelOperationalGovernance != null) { focus.addAll(kernelOperationalGovernance.nextActions()); focus.addAll(kernelOperationalGovernance.watchpoints()); }
        if (negotiationChatDigest != null) { focus.addAll(negotiationChatDigest.anchorNarratives()); focus.addAll(negotiationChatDigest.protectedTopics()); focus.addAll(negotiationChatDigest.internalActions()); }
        if (negotiationApprovalMatrix != null) { focus.addAll(negotiationApprovalMatrix.approvalGates()); focus.addAll(negotiationApprovalMatrix.releaseChecklist()); }
        if (negotiationChannelGovernance != null) { focus.addAll(negotiationChannelGovernance.participantDirectives()); focus.addAll(negotiationChannelGovernance.deliveryGuardrails()); }
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
        if (kernelDecisionMetrics != null) { focus.addAll(kernelDecisionMetrics.hotSignals()); focus.addAll(kernelDecisionMetrics.stabilitySignals()); }
        if (kernelRiskEscalation != null) { focus.addAll(kernelRiskEscalation.containmentActions()); focus.addAll(kernelRiskEscalation.recommendedLanes()); }
        if (governedMessageDecision != null) { focus.addAll(governedMessageDecision.reasons()); focus.addAll(governedMessageDecision.mandatoryActions()); }
        focus.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(focus);
    }

    private static List<String> buildNegotiationSignals(Processo processo) {
        List<String> out = new ArrayList<>();
        if (processo == null) {
            return List.of();
        }
        if (processo.getFaseAtual() != null) {
            out.add("Fase atual: " + processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            out.add("Status do processo: " + processo.getStatusProcesso().name());
        }
        return List.copyOf(out);
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty() || max <= 0) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
    }

    public record AcordoPipelineAnalysis(SettlementAdvisoryReport settlementAdvisory,
                                          InstitutionalGovernanceContextReport governance,
                                          NegotiationMemoryReport negotiationMemory,
                                          NegotiationExplainabilityReport negotiationExplainability,
                                          KernelOperationalGovernanceReport kernelOperationalGovernance,
                                          NegotiationChatDigestReport negotiationChatDigest,
                                          NegotiationApprovalMatrixReport negotiationApprovalMatrix,
                                          NegotiationChannelGovernanceReport negotiationChannelGovernance,
                                          InstitutionalPolicySnapshotReport institutionalPolicySnapshot,
                                          KernelDecisionMetricsReport kernelDecisionMetrics,
                                          KernelRiskEscalationReport kernelRiskEscalation,
                                          NegotiationMessageDecision governedMessageDecision,
                                          KernelAdvisoryTelemetry telemetry,
                                          List<String> strategicFocus,
                                          String intelligenceDigest,
                                          String suggestedNextMessage) {
    }
}
