package com.tcc.pjb.backend.core.kernel.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NegotiationGovernedChatServiceTest {

    @Test
    void shouldPersistPolicyAndRiskAwareSnapshots() {
        NegotiationMessagePreflightService preflightService = mock(NegotiationMessagePreflightService.class);
        KernelDecisionEventService kernelDecisionEventService = mock(KernelDecisionEventService.class);
        ProcessIntelligenceSnapshotService snapshotService = mock(ProcessIntelligenceSnapshotService.class);

        NegotiationGovernedChatService service = new NegotiationGovernedChatService(preflightService, kernelDecisionEventService, snapshotService);

        Processo processo = Processo.builder().id(99L).numeroUnificado("0099").build();
        PropostaAcordo proposta = PropostaAcordo.builder().id(12L).build();
        Usuario actor = Usuario.builder().id(7L).tipoUsuario(TipoUsuario.ADVOGADO).build();

        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("READY", 0.82d, true, null, List.of(), List.of(), List.of("próximo passo"), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_STABLE", 0.8d, List.of("política"), List.of(), List.of(), List.of(), List.of(), Map.of());
        NegotiationMemoryReport memory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_STABLE", 0.8d, List.of("playbook"), List.of(), List.of(), List.of(), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_STABLE", 0.8d, List.of(new NegotiationExplainabilityReport.NegotiationNode("N1", "Nó central", "MEMORY", "HIGH", List.of(), List.of(), List.of())), List.of("pergunta"), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_STABLE", 0.8d, List.of(), List.of("controle"), List.of("ação"), List.of("watch"), Map.of());
        NegotiationChatDigestReport digest = new NegotiationChatDigestReport("NEGOTIATION", "NEGOTIATION_CHAT_STABLE", 0.8d, "CONVERGING", "CLOSEOUT", "WARM", "GUIDED_RELEASE", "Mensagem segura", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("blueprint"), Map.of());
        NegotiationApprovalMatrixReport approval = new NegotiationApprovalMatrixReport("NEGOTIATION", "APPROVAL_STABLE", 0.8d, "READY_FOR_RELEASE", "GUIDED_RELEASE", List.of(), List.of(), List.of(), List.of("check"), Map.of());
        NegotiationChannelGovernanceReport channel = new NegotiationChannelGovernanceReport("NEGOTIATION", "CHANNEL_STABLE", 0.8d, "GUIDED_CHANNEL", "PERSIST_ROUND", "OPTIONAL_HANDSHAKE", List.of(), List.of(), List.of(), List.of(), List.of("guardrail"), List.of(), Map.of());
        InstitutionalPolicySnapshotReport policy = new InstitutionalPolicySnapshotReport("NEGOTIATION_POLICY", "POLICY_STABLE", 0.85d, "PJB_NEGOTIATION_CIVIL", "PROCESSO", "POLICY/2026.1", false, false, List.of("mandatório"), List.of(), List.of("guardrail"), List.of(), Map.of());
        KernelDecisionMetricsReport metrics = new KernelDecisionMetricsReport("KERNEL_DECISION_METRICS", "KERNEL_METRICS_STABLE", 0.83d, 2, 0, 0, 0, 1, List.of(), List.of("estável"), Map.of());
        KernelRiskEscalationReport risk = new KernelRiskEscalationReport("KERNEL_RISK_ESCALATION", "KERNEL_RISK_STABLE", 0.84d, "CONTROLLED", List.of("contenção"), List.of(), List.of(), Map.of());
        NegotiationMessageDecision decision = new NegotiationMessageDecision("NEGOTIATION_MESSAGE_PREFLIGHT", "NEGOTIATION_MESSAGE_APPROVED", "ALLOW_RELEASE", 0.86d, true, false, false, false, "READY_FOR_RELEASE", "GUIDED_RELEASE", "PROCESSO", "CONTROLLED", List.of("ok"), List.of("seguir"), "mensagem liberada", Map.of());
        NegotiationPreflightBundle bundle = new NegotiationPreflightBundle(settlement, governance, memory, explainability, kernel, digest, approval, channel, policy, metrics, risk, decision);

        when(preflightService.preflightDetailed(eq(processo), eq(proposta), eq(List.of()), eq("Mensagem final"), any())).thenReturn(bundle);

        NegotiationMessageDecision result = service.evaluateOutboundMessage(processo, proposta, actor, List.of(), "Mensagem final", List.of("Canal: ACORDO"));

        assertEquals(decision, result);
        verify(kernelDecisionEventService).register(processo, proposta, actor, decision);
        verify(snapshotService).saveProcessSnapshot(eq(processo), any(), eq(policy), eq(risk), eq(decision));
        verify(snapshotService).saveNegotiationRound(eq(processo), eq(proposta), eq(actor), eq(decision), any(), eq("Mensagem final"));
    }
}
