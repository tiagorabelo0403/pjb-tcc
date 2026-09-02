package com.tcc.pjb.backend.core.kernel.twin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;

class ProcessTwinPolicyGovernanceOrchestratorTest {

    private final InstitutionalPolicyResolver policyResolver = mock(InstitutionalPolicyResolver.class);
    private final KernelDecisionMetricsService decisionMetricsService = mock(KernelDecisionMetricsService.class);
    private final KernelRiskEscalationService riskEscalationService = mock(KernelRiskEscalationService.class);
    private final NegotiationReleaseGuard releaseGuard = mock(NegotiationReleaseGuard.class);
    private final ProcessTwinPolicyGovernanceOrchestrator orchestrator = new ProcessTwinPolicyGovernanceOrchestrator(
            policyResolver, decisionMetricsService, riskEscalationService, releaseGuard);

    @Test
    void encadeiaPolicyMetricsEscalationEDecideAMensagemComSuggestedNextMessage() {
        Processo processo = Processo.builder().id(11L).build();
        var proposal = mock(PropostaAcordo.class);
        List<ChatMensagem> chat = List.of();
        var govContext = mock(InstitutionalGovernanceContextReport.class);
        var chatDigest = mock(NegotiationChatDigestReport.class);
        var approvalMatrix = mock(NegotiationApprovalMatrixReport.class);
        var channelGovernance = mock(NegotiationChannelGovernanceReport.class);
        var policySnapshot = mock(InstitutionalPolicySnapshotReport.class);
        var decisionMetrics = mock(KernelDecisionMetricsReport.class);
        var riskEscalation = mock(KernelRiskEscalationReport.class);
        var messageDecision = mock(NegotiationMessageDecision.class);
        when(policyResolver.resolve(processo, proposal, chat, govContext, chatDigest, approvalMatrix, channelGovernance, "COMUM_ORDINARIO")).thenReturn(policySnapshot);
        when(decisionMetricsService.analyzeProcess(processo)).thenReturn(decisionMetrics);
        when(riskEscalationService.analyzeProcess(processo, policySnapshot, decisionMetrics, chatDigest, approvalMatrix, channelGovernance)).thenReturn(riskEscalation);
        when(chatDigest.suggestedNextMessage()).thenReturn("Mensagem sugerida");
        when(releaseGuard.decide("Mensagem sugerida", policySnapshot, decisionMetrics, riskEscalation, chatDigest, approvalMatrix, channelGovernance)).thenReturn(messageDecision);

        var bundle = orchestrator.analyzeProcess(processo, "COMUM_ORDINARIO", proposal, chat, govContext, chatDigest, approvalMatrix, channelGovernance);

        assertThat(bundle.policySnapshot()).isSameAs(policySnapshot);
        assertThat(bundle.decisionMetrics()).isSameAs(decisionMetrics);
        assertThat(bundle.riskEscalation()).isSameAs(riskEscalation);
        assertThat(bundle.governedMessageDecision()).isSameAs(messageDecision);
        verify(releaseGuard).decide("Mensagem sugerida", policySnapshot, decisionMetrics, riskEscalation, chatDigest, approvalMatrix, channelGovernance);
    }

    @Test
    void quandoChatDigestNuloOReleaseGuardRecebeNullComoOutboundMessage() {
        Processo processo = Processo.builder().id(12L).build();
        var proposal = mock(PropostaAcordo.class);
        List<ChatMensagem> chat = List.of();
        var govContext = mock(InstitutionalGovernanceContextReport.class);
        var approvalMatrix = mock(NegotiationApprovalMatrixReport.class);
        var channelGovernance = mock(NegotiationChannelGovernanceReport.class);
        var policySnapshot = mock(InstitutionalPolicySnapshotReport.class);
        var decisionMetrics = mock(KernelDecisionMetricsReport.class);
        var riskEscalation = mock(KernelRiskEscalationReport.class);
        var messageDecision = mock(NegotiationMessageDecision.class);
        when(policyResolver.resolve(processo, proposal, chat, govContext, null, approvalMatrix, channelGovernance, "COMUM_ORDINARIO")).thenReturn(policySnapshot);
        when(decisionMetricsService.analyzeProcess(processo)).thenReturn(decisionMetrics);
        when(riskEscalationService.analyzeProcess(processo, policySnapshot, decisionMetrics, null, approvalMatrix, channelGovernance)).thenReturn(riskEscalation);
        when(releaseGuard.decide(null, policySnapshot, decisionMetrics, riskEscalation, null, approvalMatrix, channelGovernance)).thenReturn(messageDecision);

        var bundle = orchestrator.analyzeProcess(processo, "COMUM_ORDINARIO", proposal, chat, govContext, null, approvalMatrix, channelGovernance);

        assertThat(bundle.governedMessageDecision()).isSameAs(messageDecision);
    }
}
