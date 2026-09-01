package com.tcc.pjb.backend.core.kernel.twin;

import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
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
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de ProcessDigitalTwinService: resolução de política institucional,
 * métricas de decisão do kernel, escalação de risco e liberação governada de mensagem
 * de negociação. Todos os 4 serviços operam sobre o resultado da negociação já composto
 * pelo NegotiationOrchestrator (recebido como parâmetros do Bundle).
 */
@Service
public class ProcessTwinPolicyGovernanceOrchestrator {

    private final InstitutionalPolicyResolver institutionalPolicyResolver;
    private final KernelDecisionMetricsService kernelDecisionMetricsService;
    private final KernelRiskEscalationService kernelRiskEscalationService;
    private final NegotiationReleaseGuard negotiationReleaseGuard;

    public ProcessTwinPolicyGovernanceOrchestrator(InstitutionalPolicyResolver institutionalPolicyResolver,
                                                    KernelDecisionMetricsService kernelDecisionMetricsService,
                                                    KernelRiskEscalationService kernelRiskEscalationService,
                                                    NegotiationReleaseGuard negotiationReleaseGuard) {
        this.institutionalPolicyResolver = Objects.requireNonNull(institutionalPolicyResolver);
        this.kernelDecisionMetricsService = Objects.requireNonNull(kernelDecisionMetricsService);
        this.kernelRiskEscalationService = Objects.requireNonNull(kernelRiskEscalationService);
        this.negotiationReleaseGuard = Objects.requireNonNull(negotiationReleaseGuard);
    }

    public Bundle analyzeProcess(Processo processo,
                                  String ritoCode,
                                  PropostaAcordo latestProposal,
                                  List<ChatMensagem> recentChat,
                                  InstitutionalGovernanceContextReport institutionalGovernanceContext,
                                  NegotiationChatDigestReport chatDigest,
                                  NegotiationApprovalMatrixReport approvalMatrix,
                                  NegotiationChannelGovernanceReport channelGovernance) {
        InstitutionalPolicySnapshotReport policySnapshot = institutionalPolicyResolver.resolve(processo, latestProposal, recentChat, institutionalGovernanceContext, chatDigest, approvalMatrix, channelGovernance, ritoCode);
        KernelDecisionMetricsReport decisionMetrics = kernelDecisionMetricsService.analyzeProcess(processo);
        KernelRiskEscalationReport riskEscalation = kernelRiskEscalationService.analyzeProcess(processo, policySnapshot, decisionMetrics, chatDigest, approvalMatrix, channelGovernance);
        NegotiationMessageDecision governedMessageDecision = negotiationReleaseGuard.decide(
                chatDigest != null ? chatDigest.suggestedNextMessage() : null,
                policySnapshot,
                decisionMetrics,
                riskEscalation,
                chatDigest,
                approvalMatrix,
                channelGovernance
        );
        return new Bundle(policySnapshot, decisionMetrics, riskEscalation, governedMessageDecision);
    }

    public record Bundle(
            InstitutionalPolicySnapshotReport policySnapshot,
            KernelDecisionMetricsReport decisionMetrics,
            KernelRiskEscalationReport riskEscalation,
            NegotiationMessageDecision governedMessageDecision
    ) {
    }
}
