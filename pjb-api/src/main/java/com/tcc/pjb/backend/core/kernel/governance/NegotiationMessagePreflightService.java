package com.tcc.pjb.backend.core.kernel.governance;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextService;
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
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationLanguageHeuristics;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NegotiationMessagePreflightService {

    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
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

    public NegotiationMessageDecision preflight(Processo processo,
                                                PropostaAcordo proposta,
                                                List<ChatMensagem> recentChat,
                                                String outboundMessage,
                                                List<String> negotiationSignals) {
        return preflightDetailed(processo, proposta, recentChat, outboundMessage, negotiationSignals).decision();
    }

    @SuppressWarnings("DataFlowIssue")
    public NegotiationPreflightBundle preflightDetailed(Processo processo,
                                                        PropostaAcordo proposta,
                                                        List<ChatMensagem> recentChat,
                                                        String outboundMessage,
                                                        List<String> negotiationSignals) {
        Objects.requireNonNull(processo, "processo");
        List<ChatMensagem> normalizedChat = normalizeChat(recentChat);
        List<String> normalizedSignals = normalizeSignals(negotiationSignals, outboundMessage);
        String normalizedOutboundMessage = normalizeOutboundMessage(outboundMessage);
        String ritoName = processoRitoSnapshotService.resolve(processo, null).ritoCode();
        SettlementAdvisoryReport settlementAdvisory = settlementAdvisoryService.analyze(
                processo,
                ritoName,
                proposta != null ? proposta.getValorAcordo() : processo.getValorCausa(),
                normalizedSignals,
                null
        );
        InstitutionalGovernanceContextReport governance = institutionalGovernanceContextService.analyzeProcess(
                processo,
                ritoName,
                settlementAdvisory,
                null,
                null
        );
        NegotiationMemoryReport negotiationMemory = negotiationMemoryService.analyzeProcess(
                processo,
                proposta,
                normalizedChat,
                settlementAdvisory,
                governance
        );
        NegotiationExplainabilityReport negotiationExplainability = negotiationExplainabilityService.compose(
                processo,
                proposta,
                normalizedChat,
                settlementAdvisory,
                negotiationMemory,
                governance
        );
        KernelOperationalGovernanceReport kernelOperationalGovernance = kernelOperationalGovernanceService.analyzeProcess(
                processo,
                ritoName,
                null,
                null,
                governance,
                negotiationMemory,
                negotiationExplainability,
                null,
                null
        );
        NegotiationChatDigestReport chatDigest = negotiationChatDigestService.analyzeProcess(
                processo,
                proposta,
                normalizedChat,
                settlementAdvisory,
                negotiationMemory,
                negotiationExplainability,
                governance,
                kernelOperationalGovernance
        );
        NegotiationApprovalMatrixReport approvalMatrix = negotiationApprovalMatrixService.analyzeProcess(
                processo,
                proposta,
                normalizedChat,
                governance,
                kernelOperationalGovernance,
                negotiationMemory,
                negotiationExplainability,
                chatDigest
        );
        NegotiationChannelGovernanceReport channelGovernance = negotiationChannelGovernanceService.analyzeProcess(
                processo,
                proposta,
                normalizedChat,
                governance,
                kernelOperationalGovernance,
                negotiationMemory,
                negotiationExplainability,
                chatDigest,
                approvalMatrix
        );
        InstitutionalPolicySnapshotReport policy = institutionalPolicyResolver.resolve(
                processo,
                proposta,
                normalizedChat,
                governance,
                chatDigest,
                approvalMatrix,
                channelGovernance,
                ritoName
        );
        KernelDecisionMetricsReport metrics = kernelDecisionMetricsService.analyzeProcess(processo);
        KernelRiskEscalationReport riskEscalation = kernelRiskEscalationService.analyzeProcess(
                processo,
                policy,
                metrics,
                chatDigest,
                approvalMatrix,
                channelGovernance
        );
        NegotiationMessageDecision decision = negotiationReleaseGuard.decide(
                normalizedOutboundMessage,
                policy,
                metrics,
                riskEscalation,
                chatDigest,
                approvalMatrix,
                channelGovernance
        );
        return new NegotiationPreflightBundle(
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                chatDigest,
                approvalMatrix,
                channelGovernance,
                policy,
                metrics,
                riskEscalation,
                decision
        );
    }

    private List<ChatMensagem> normalizeChat(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return List.of();
        }
        return recentChat.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatMensagem::getDataEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<String> normalizeSignals(List<String> negotiationSignals, String outboundMessage) {
        java.util.LinkedHashSet<String> signals = new java.util.LinkedHashSet<>();
        if (negotiationSignals != null) {
            negotiationSignals.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(signals::add);
        }
        String normalizedOutbound = normalizeOutboundMessage(outboundMessage);
        if (!normalizedOutbound.isBlank()) {
            signals.add("Mensagem candidata: " + normalizedOutbound);
            String lower = normalizedOutbound.toLowerCase(Locale.ROOT);
            if (containsNegotiationMarker(lower) || lower.contains("proposta")) {
                signals.add("A rodada atual tem intenção negocial explícita.");
            }
            if (lower.contains("parcel") || lower.contains("cronograma")) {
                signals.add("A rodada atual toca estrutura de pagamento ou execução.");
            }
            if (lower.contains("sigilo") || lower.contains("confidencial")) {
                signals.add("A rodada atual exige contenção informacional reforçada.");
            }
        }
        return List.copyOf(signals);
    }

    private boolean containsNegotiationMarker(String lower) {
        if (lower == null || lower.isBlank()) {
            return false;
        }
        return NegotiationLanguageHeuristics.containsPositiveSettlementSignal(lower);
    }

    private String normalizeOutboundMessage(String outboundMessage) {
        if (outboundMessage == null) {
            return "";
        }
        return outboundMessage.trim();
    }
}
