package com.tcc.pjb.backend.core.kernel.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.model.entity.Processo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KernelRiskEscalationServiceTest {

    private final KernelRiskEscalationService service = new KernelRiskEscalationService();

    @Test
    void shouldEscalateWhenPolicyAndApprovalAreSevere() {
        Processo processo = Processo.builder().id(99L).numeroUnificado("0099").build();
        InstitutionalPolicySnapshotReport policy = new InstitutionalPolicySnapshotReport("NEGOTIATION_POLICY", "POLICY_ATTENTION", 0.62d, "PJB_NEGOTIATION_CIVIL", "PROCESSO", "POLICY/2026.1", true, true, List.of("aprovar"), List.of("bloquear"), List.of("guardrail"), List.of("escalar"), Map.of());
        KernelDecisionMetricsReport metrics = new KernelDecisionMetricsReport("KERNEL_DECISION_METRICS", "KERNEL_METRICS_ATTENTION", 0.66d, 8, 3, 2, 1, 4, List.of("bloqueios recentes"), List.of(), Map.of());
        NegotiationChatDigestReport digest = new NegotiationChatDigestReport("NEGOTIATION_CHAT", "NEGOTIATION_CHAT_ATTENTION", 0.5d, "IMPASSE", "DEESCALATION", "HOT", "BLOCKED_RELEASE", "mensagem", List.of(), List.of(), List.of("escalar"), List.of(), List.of(), List.of("revisar"), List.of(), Map.of());
        NegotiationApprovalMatrixReport approval = new NegotiationApprovalMatrixReport("NEGOTIATION", "APPROVAL_ATTENTION", 0.5d, "EXECUTIVE_ESCALATION", "BLOCKED_RELEASE", List.of("gate"), List.of("executivo"), List.of("controle"), List.of("checklist"), Map.of());
        NegotiationChannelGovernanceReport channel = new NegotiationChannelGovernanceReport("NEGOTIATION", "CHANNEL_ATTENTION", 0.5d, "STRICT_AUDIT_CHANNEL", "INTERNAL_DRAFT_ONLY", "APPROVAL_HANDSHAKE_REQUIRED", List.of(), List.of(), List.of(), List.of(), List.of("guardrail"), List.of("fallback"), Map.of());

        KernelRiskEscalationReport report = service.analyzeProcess(processo, policy, metrics, digest, approval, channel);

        assertEquals("CRITICAL", report.escalationLevel());
        assertFalse(report.containmentActions().isEmpty());
        assertFalse(report.recommendedLanes().isEmpty());
    }
}
