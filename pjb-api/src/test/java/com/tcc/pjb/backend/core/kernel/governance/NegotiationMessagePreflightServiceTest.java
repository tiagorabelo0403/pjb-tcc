package com.tcc.pjb.backend.core.kernel.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NegotiationMessagePreflightServiceTest {

    @Test
    void shouldBuildDetailedBundleWithNormalizedSignals() {
        ProcessoRitoSnapshotService ritoService = mock(ProcessoRitoSnapshotService.class);
        SettlementAdvisoryService settlementService = mock(SettlementAdvisoryService.class);
        InstitutionalGovernanceContextService governanceService = mock(InstitutionalGovernanceContextService.class);
        NegotiationMemoryService memoryService = mock(NegotiationMemoryService.class);
        NegotiationExplainabilityService explainabilityService = mock(NegotiationExplainabilityService.class);
        NegotiationChatDigestService chatDigestService = mock(NegotiationChatDigestService.class);
        NegotiationApprovalMatrixService approvalMatrixService = mock(NegotiationApprovalMatrixService.class);
        NegotiationChannelGovernanceService channelGovernanceService = mock(NegotiationChannelGovernanceService.class);
        KernelOperationalGovernanceService kernelOperationalGovernanceService = mock(KernelOperationalGovernanceService.class);
        InstitutionalPolicyResolver institutionalPolicyResolver = mock(InstitutionalPolicyResolver.class);
        KernelDecisionMetricsService kernelDecisionMetricsService = mock(KernelDecisionMetricsService.class);
        KernelRiskEscalationService kernelRiskEscalationService = mock(KernelRiskEscalationService.class);
        NegotiationReleaseGuard releaseGuard = mock(NegotiationReleaseGuard.class);

        NegotiationMessagePreflightService service = new NegotiationMessagePreflightService(
                ritoService,
                settlementService,
                governanceService,
                memoryService,
                explainabilityService,
                chatDigestService,
                approvalMatrixService,
                channelGovernanceService,
                kernelOperationalGovernanceService,
                institutionalPolicyResolver,
                kernelDecisionMetricsService,
                kernelRiskEscalationService,
                releaseGuard
        );

        Processo processo = Processo.builder().id(17L).numeroUnificado("0017").faseAtual(FaseProcessual.CONHECIMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder().id(2L).valorAcordo(BigDecimal.valueOf(2500)).build();
        ChatMensagem chat = ChatMensagem.builder().conteudo("Podemos conversar sobre acordo com parcelamento").dataEnvio(LocalDateTime.now()).build();

        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("READY", 0.84d, true, null, List.of(), List.of(), List.of("Avançar com síntese final"), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_STABLE", 0.81d, List.of("Blindar linguagem institucional"), List.of(), List.of("Submeter versão final"), List.of(), List.of(), Map.of());
        NegotiationMemoryReport memory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_STABLE", 0.8d, List.of("Usar playbook de parcelamento"), List.of(), List.of(), List.of(), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_STABLE", 0.79d, List.of(), List.of("Checar aceite final"), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_STABLE", 0.8d, List.of(), List.of("controle:mensagem"), List.of("revisar versão final"), List.of(), Map.of());
        NegotiationChatDigestReport digest = new NegotiationChatDigestReport("NEGOTIATION", "NEGOTIATION_CHAT_STABLE", 0.8d, "CONVERGING", "CLOSEOUT", "WARM", "GUIDED_RELEASE", "Mensagem segura", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        NegotiationApprovalMatrixReport approval = new NegotiationApprovalMatrixReport("NEGOTIATION", "APPROVAL_STABLE", 0.81d, "READY_FOR_RELEASE", "GUIDED_RELEASE", List.of(), List.of(), List.of(), List.of("Checklist final"), Map.of());
        NegotiationChannelGovernanceReport channel = new NegotiationChannelGovernanceReport("NEGOTIATION", "CHANNEL_STABLE", 0.82d, "GUIDED_CHANNEL", "PERSIST_ROUND", "OPTIONAL_HANDSHAKE", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        InstitutionalPolicySnapshotReport policy = new InstitutionalPolicySnapshotReport("NEGOTIATION_POLICY", "POLICY_STABLE", 0.83d, "PJB_NEGOTIATION_CIVIL", "PROCESSO", "POLICY/2026.1", false, false, List.of("Conferir valor"), List.of(), List.of("Guardrail"), List.of(), Map.of());
        KernelDecisionMetricsReport metrics = new KernelDecisionMetricsReport("KERNEL_DECISION_METRICS", "KERNEL_METRICS_STABLE", 0.84d, 4, 0, 0, 0, 1, List.of(), List.of("Série estável"), Map.of());
        KernelRiskEscalationReport risk = new KernelRiskEscalationReport("KERNEL_RISK_ESCALATION", "KERNEL_RISK_STABLE", 0.85d, "CONTROLLED", List.of("Conter ruído"), List.of(), List.of(), Map.of());
        NegotiationMessageDecision decision = new NegotiationMessageDecision("NEGOTIATION_MESSAGE_PREFLIGHT", "NEGOTIATION_MESSAGE_APPROVED", "ALLOW_RELEASE", 0.84d, true, false, false, false, "READY_FOR_RELEASE", "GUIDED_RELEASE", "PROCESSO", "CONTROLLED", List.of("ok"), List.of("seguir"), "mensagem liberada", Map.of());

        when(ritoService.resolve(eq(processo), eq(null))).thenReturn(new ProcessoRitoSnapshotService.ProcessoRitoSnapshot(null, "COMUM_ORDINARIO", null, null, null, false, List.of(), null, false));
        when(settlementService.analyze(eq(processo), eq("COMUM_ORDINARIO"), eq(BigDecimal.valueOf(2500)), any(), eq(null))).thenReturn(settlement);
        when(governanceService.analyzeProcess(eq(processo), eq("COMUM_ORDINARIO"), eq(settlement), eq(null), eq(null))).thenReturn(governance);
        when(memoryService.analyzeProcess(eq(processo), eq(proposta), any(), eq(settlement), eq(governance))).thenReturn(memory);
        when(explainabilityService.compose(eq(processo), eq(proposta), any(), eq(settlement), eq(memory), eq(governance))).thenReturn(explainability);
        when(kernelOperationalGovernanceService.analyzeProcess(eq(processo), eq("COMUM_ORDINARIO"), eq(null), eq(null), eq(governance), eq(memory), eq(explainability), eq(null), eq(null))).thenReturn(kernel);
        when(chatDigestService.analyzeProcess(eq(processo), eq(proposta), any(), eq(settlement), eq(memory), eq(explainability), eq(governance), eq(kernel))).thenReturn(digest);
        when(approvalMatrixService.analyzeProcess(eq(processo), eq(proposta), any(), eq(governance), eq(kernel), eq(memory), eq(explainability), eq(digest))).thenReturn(approval);
        when(channelGovernanceService.analyzeProcess(eq(processo), eq(proposta), any(), eq(governance), eq(kernel), eq(memory), eq(explainability), eq(digest), eq(approval))).thenReturn(channel);
        when(institutionalPolicyResolver.resolve(eq(processo), eq(proposta), any(), eq(governance), eq(digest), eq(approval), eq(channel), eq("COMUM_ORDINARIO"))).thenReturn(policy);
        when(kernelDecisionMetricsService.analyzeProcess(eq(processo))).thenReturn(metrics);
        when(kernelRiskEscalationService.analyzeProcess(eq(processo), eq(policy), eq(metrics), eq(digest), eq(approval), eq(channel))).thenReturn(risk);
        when(releaseGuard.decide(eq("Vamos fechar com acordo de parcelamento sigiloso"), eq(policy), eq(metrics), eq(risk), eq(digest), eq(approval), eq(channel))).thenReturn(decision);

        NegotiationPreflightBundle bundle = service.preflightDetailed(
                processo,
                proposta,
                List.of(chat),
                "  Vamos fechar com acordo de parcelamento sigiloso  ",
                List.of("Canal: ACORDO")
        );

        assertNotNull(bundle);
        assertEquals(decision, bundle.decision());
        assertEquals(policy, bundle.institutionalPolicySnapshot());
        assertEquals(risk, bundle.kernelRiskEscalation());
        assertFalse(bundle.decision().mandatoryActions().isEmpty());
    }
}
