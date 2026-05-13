package com.tcc.pjb.backend.core.kernel.governance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.util.PayloadMaps;

@Service
public class NegotiationReleaseGuard {

    public NegotiationMessageDecision decide(String outboundMessage,
                                             InstitutionalPolicySnapshotReport policy,
                                             KernelDecisionMetricsReport metrics,
                                             KernelRiskEscalationReport riskEscalation,
                                             NegotiationChatDigestReport chatDigest,
                                             NegotiationApprovalMatrixReport approvalMatrix,
                                             NegotiationChannelGovernanceReport channelGovernance) {
        Set<String> reasons = new LinkedHashSet<>();
        Set<String> mandatoryActions = new LinkedHashSet<>();
        String normalized = normalize(outboundMessage);
        double confidence = 0.72d;
        boolean approvalRequired = policy != null && policy.approvalRequired();
        boolean strictMode = policy != null && policy.strictRelease();
        boolean internalDraftRequired = false;
        boolean releaseAllowed = true;
        String decisionCode = "ALLOW_RELEASE";
        String approvalBand = approvalMatrix != null ? approvalMatrix.approvalBand() : null;
        String releaseMode = approvalMatrix != null ? approvalMatrix.releaseMode() : chatDigest != null ? chatDigest.sendMode() : "GUIDED_RELEASE";
        String riskLevel = riskEscalation != null ? riskEscalation.escalationLevel() : "CONTROLLED";

        if (policy != null) {
            mandatoryActions.addAll(policy.mandatoryDirectives());
            mandatoryActions.addAll(policy.releaseGuardrails());
            if (containsRestrictedDirective(normalized, policy.blockingDirectives())) {
                releaseAllowed = false;
                strictMode = true;
                decisionCode = "BLOCK_RELEASE_POLICY";
                reasons.add("A mensagem proposta colide com diretiva bloqueante da política institucional efetiva.");
            }
        }
        if (chatDigest != null) {
            mandatoryActions.addAll(chatDigest.internalActions());
            if (containsRestrictedDirective(normalized, chatDigest.forbiddenMoves())) {
                releaseAllowed = false;
                decisionCode = "BLOCK_RELEASE_CONVERSATION";
                reasons.add("O conteúdo proposto repete movimento que o digest negocial classificou como vedado.");
            }
            if ("IMPASSE".equals(chatDigest.conversationStage())) {
                internalDraftRequired = true;
                mandatoryActions.add("Reescrever a abordagem com linguagem de deescalation antes de enviar nova mensagem externa.");
            }
        }
        if (approvalMatrix != null) {
            mandatoryActions.addAll(approvalMatrix.releaseChecklist());
            mandatoryActions.addAll(approvalMatrix.internalControls());
            if (!approvalMatrix.approvalGates().isEmpty()) {
                approvalRequired = true;
            }
            if ("BLOCKED_RELEASE".equals(approvalMatrix.releaseMode())) {
                releaseAllowed = false;
                decisionCode = "BLOCK_RELEASE_APPROVAL";
                reasons.add("A matriz de aprovação bloqueou a liberação da rodada até que as gates sejam vencidas.");
            }
            if (!releaseAllowed && !approvalMatrix.escalationLanes().isEmpty()) {
                mandatoryActions.addAll(approvalMatrix.escalationLanes());
            }
        }
        if (channelGovernance != null) {
            mandatoryActions.addAll(channelGovernance.deliveryGuardrails());
            if ("APPROVAL_HANDSHAKE_REQUIRED".equals(channelGovernance.approvalHandshake())) {
                approvalRequired = true;
            }
            if ("INTERNAL_DRAFT_ONLY".equals(channelGovernance.persistenceMode())) {
                internalDraftRequired = true;
            }
        }
        if (metrics != null && metrics.blockedDecisions() >= 2) {
            strictMode = true;
            mandatoryActions.add("Registrar justificativa reforçada da liberação após série recente de bloqueios do kernel.");
            confidence -= 0.06d;
        }
        if (riskEscalation != null) {
            mandatoryActions.addAll(riskEscalation.containmentActions());
            mandatoryActions.addAll(riskEscalation.recommendedLanes());
            if ("CRITICAL".equals(riskEscalation.escalationLevel()) || "HIGH".equals(riskEscalation.escalationLevel())) {
                releaseAllowed = false;
                internalDraftRequired = true;
                decisionCode = "BLOCK_RELEASE_RISK";
                reasons.add("O nível de escalada de risco do kernel exige contenção antes de nova interação externa.");
            }
        }
        if (approvalRequired && releaseAllowed && strictMode) {
            releaseAllowed = false;
            decisionCode = "REQUIRE_APPROVAL";
            reasons.add("A política efetiva exige aprovação formal antes da liberação em modo estrito.");
        }
        if (!releaseAllowed && internalDraftRequired && !Objects.equals(decisionCode, "BLOCK_RELEASE_RISK")) {
            decisionCode = "INTERNAL_DRAFT_REQUIRED";
        }
        if (reasons.isEmpty()) {
            reasons.add("A mensagem proposta respeita a governança vigente, com liberação controlada pelo kernel.");
            confidence += 0.05d;
        }

        String status = releaseAllowed ? "NEGOTIATION_MESSAGE_APPROVED" : "NEGOTIATION_MESSAGE_HELD";
        String releaseMessage = releaseAllowed
                ? "Mensagem liberada em modo governado: " + safeTrim(outboundMessage)
                : "Mensagem retida em governança: converter em rascunho interno e cumprir as ações mandatórias antes de novo envio.";
        return new NegotiationMessageDecision(
                "NEGOTIATION_MESSAGE_PREFLIGHT",
                status,
                decisionCode,
                round(clamp(confidence)),
                releaseAllowed,
                approvalRequired,
                internalDraftRequired,
                strictMode,
                approvalBand,
                releaseMode,
                policy != null ? policy.policyTier() : null,
                riskLevel,
                List.copyOf(reasons),
                List.copyOf(mandatoryActions),
                releaseMessage,
                PayloadMaps.ofEntries(
                        "approvalRequired", approvalRequired,
                        "strictMode", strictMode,
                        "releaseAllowed", releaseAllowed,
                        "internalDraftRequired", internalDraftRequired,
                        "approvalBand", approvalBand,
                        "releaseMode", releaseMode,
                        "riskLevel", riskLevel,
                        "messageLength", outboundMessage != null ? outboundMessage.length() : 0
                )
        );
    }

    private boolean containsRestrictedDirective(String normalizedMessage, List<String> directives) {
        if (normalizedMessage == null || normalizedMessage.isBlank() || directives == null || directives.isEmpty()) {
            return false;
        }
        return directives.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .anyMatch(normalizedMessage::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('á', 'a').replace('à', 'a').replace('ã', 'a').replace('â', 'a')
                .replace('é', 'e').replace('ê', 'e').replace('í', 'i').replace('ó', 'o').replace('ô', 'o').replace('õ', 'o').replace('ú', 'u').replace('ç', 'c');
    }

    private String safeTrim(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 240 ? trimmed : trimmed.substring(0, 240);
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(0.99d, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
