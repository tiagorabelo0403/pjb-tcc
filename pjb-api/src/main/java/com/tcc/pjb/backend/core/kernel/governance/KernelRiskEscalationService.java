package com.tcc.pjb.backend.core.kernel.governance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;

@Service
public class KernelRiskEscalationService {

    public KernelRiskEscalationReport analyzeProcess(Processo processo,
                                                     InstitutionalPolicySnapshotReport policy,
                                                     KernelDecisionMetricsReport metrics,
                                                     NegotiationChatDigestReport chatDigest,
                                                     NegotiationApprovalMatrixReport approvalMatrix,
                                                     NegotiationChannelGovernanceReport channelGovernance) {
        Set<String> containmentActions = new LinkedHashSet<>();
        Set<String> escalationTriggers = new LinkedHashSet<>();
        Set<String> recommendedLanes = new LinkedHashSet<>();
        double score = 0.22d;

        if (policy != null) {
            containmentActions.addAll(policy.releaseGuardrails());
            escalationTriggers.addAll(policy.escalationTriggers());
            if (policy.strictRelease()) {
                score += 0.24d;
            }
            if (policy.approvalRequired()) {
                score += 0.16d;
                recommendedLanes.add("Escalar validação para alçada responsável definida pela política institucional.");
            }
        }
        if (metrics != null) {
            containmentActions.addAll(metrics.hotSignals());
            if (metrics.blockedDecisions() > 0) {
                score += 0.18d;
            }
            if (metrics.approvalRequiredDecisions() > 0) {
                score += 0.12d;
            }
        }
        if (chatDigest != null) {
            escalationTriggers.addAll(chatDigest.escalationSignals());
            containmentActions.addAll(chatDigest.internalActions());
            if ("IMPASSE".equals(chatDigest.conversationStage())) {
                score += 0.18d;
                recommendedLanes.add("Migrar a rodada para deescalation assistida antes de novo fechamento no chat.");
            }
            if ("BLOCKED_RELEASE".equals(chatDigest.sendMode())) {
                score += 0.15d;
            }
        }
        if (approvalMatrix != null) {
            escalationTriggers.addAll(approvalMatrix.approvalGates());
            escalationTriggers.addAll(approvalMatrix.escalationLanes());
            containmentActions.addAll(approvalMatrix.internalControls());
            if ("EXECUTIVE_ESCALATION".equals(approvalMatrix.approvalBand())) {
                score += 0.20d;
            }
            if ("BLOCKED_RELEASE".equals(approvalMatrix.releaseMode())) {
                score += 0.15d;
            }
        }
        if (channelGovernance != null) {
            containmentActions.addAll(channelGovernance.deliveryGuardrails());
            recommendedLanes.addAll(channelGovernance.fallbackLanes());
            if ("STRICT_AUDIT_CHANNEL".equals(channelGovernance.operatingMode())) {
                score += 0.08d;
            }
        }

        String escalationLevel = score >= 0.75d ? "CRITICAL" : score >= 0.50d ? "HIGH" : score >= 0.30d ? "MODERATE" : "CONTROLLED";
        String status = switch (escalationLevel) {
            case "CRITICAL", "HIGH" -> "KERNEL_RISK_ATTENTION";
            default -> "KERNEL_RISK_STABLE";
        };
        return new KernelRiskEscalationReport(
                "KERNEL_RISK_ESCALATION",
                status,
                round(clamp(1.0d - (score / 1.25d))),
                escalationLevel,
                List.copyOf(containmentActions),
                List.copyOf(escalationTriggers),
                List.copyOf(recommendedLanes),
                PayloadMaps.ofEntries(
                        "processoId", processo.getId(),
                        "escalationLevel", escalationLevel,
                        "score", round(score)
                )
        );
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(0.99d, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
