package com.tcc.pjb.backend.core.kernel.governance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NegotiationReleaseGuardTest {

    private final NegotiationReleaseGuard service = new NegotiationReleaseGuard();

    @Test
    void shouldHoldMessageWhenRiskIsHigh() {
        InstitutionalPolicySnapshotReport policy = new InstitutionalPolicySnapshotReport("NEGOTIATION_POLICY", "POLICY_ATTENTION", 0.6d, "PJB_NEGOTIATION_CIVIL", "PROCESSO", "POLICY/2026.1", true, true, List.of("aprovar"), List.of("não soltar números"), List.of("validar checklist"), List.of("escalar"), Map.of());
        KernelDecisionMetricsReport metrics = new KernelDecisionMetricsReport("KERNEL_DECISION_METRICS", "KERNEL_METRICS_ATTENTION", 0.6d, 5, 2, 1, 1, 3, List.of("bloqueio recente"), List.of(), Map.of());
        KernelRiskEscalationReport risk = new KernelRiskEscalationReport("KERNEL_RISK_ESCALATION", "KERNEL_RISK_ATTENTION", 0.4d, "HIGH", List.of("revisar"), List.of("escalar"), List.of("fallback"), Map.of());
        NegotiationChatDigestReport digest = new NegotiationChatDigestReport("NEGOTIATION_CHAT", "NEGOTIATION_CHAT_ATTENTION", 0.5d, "IMPASSE", "DEESCALATION", "HOT", "GUIDED_RELEASE", "mensagem", List.of(), List.of(), List.of("escalar"), List.of(), List.of("não soltar números"), List.of("revisar"), List.of(), Map.of());
        NegotiationApprovalMatrixReport approval = new NegotiationApprovalMatrixReport("NEGOTIATION", "APPROVAL_ATTENTION", 0.5d, "EXECUTIVE_ESCALATION", "BLOCKED_RELEASE", List.of("gate"), List.of("executivo"), List.of("controle"), List.of("checklist"), Map.of());
        NegotiationChannelGovernanceReport channel = new NegotiationChannelGovernanceReport("NEGOTIATION", "CHANNEL_ATTENTION", 0.5d, "STRICT_AUDIT_CHANNEL", "INTERNAL_DRAFT_ONLY", "APPROVAL_HANDSHAKE_REQUIRED", List.of(), List.of(), List.of(), List.of(), List.of("guardrail"), List.of("fallback"), Map.of());

        NegotiationMessageDecision decision = service.decide("Vamos fechar agora com números finais", policy, metrics, risk, digest, approval, channel);

        assertFalse(decision.releaseAllowed());
        assertTrue(decision.internalDraftRequired());
        assertFalse(decision.mandatoryActions().isEmpty());
    }
}
