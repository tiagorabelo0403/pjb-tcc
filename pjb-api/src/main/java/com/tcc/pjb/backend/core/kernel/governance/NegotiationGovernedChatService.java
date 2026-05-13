package com.tcc.pjb.backend.core.kernel.governance;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityReport;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NegotiationGovernedChatService {

    private final NegotiationMessagePreflightService preflightService;
    private final KernelDecisionEventService kernelDecisionEventService;
    private final ProcessIntelligenceSnapshotService snapshotService;

    @Transactional
    public NegotiationMessageDecision evaluateOutboundMessage(Processo processo,
                                                             PropostaAcordo proposta,
                                                             Usuario actor,
                                                             List<ChatMensagem> recentChat,
                                                             String outboundMessage,
                                                             List<String> negotiationSignals) {
        Objects.requireNonNull(processo, "processo");
        NegotiationPreflightBundle bundle = preflightService.preflightDetailed(
                processo,
                proposta,
                recentChat,
                outboundMessage,
                enrichSignals(processo, actor, proposta, recentChat, outboundMessage, negotiationSignals)
        );
        NegotiationMessageDecision decision = bundle.decision();
        List<String> strategicFocus = buildStrategicFocus(bundle, outboundMessage, actor, recentChat);
        kernelDecisionEventService.register(processo, proposta, actor, decision);
        snapshotService.saveProcessSnapshot(
                processo,
                strategicFocus,
                bundle.institutionalPolicySnapshot(),
                bundle.kernelRiskEscalation(),
                decision
        );
        snapshotService.saveNegotiationRound(
                processo,
                proposta,
                actor,
                decision,
                strategicFocus,
                resolvePersistedSuggestedMessage(bundle, outboundMessage)
        );
        return decision;
    }

    private List<String> enrichSignals(Processo processo,
                                       Usuario actor,
                                       PropostaAcordo proposta,
                                       List<ChatMensagem> recentChat,
                                       String outboundMessage,
                                       List<String> negotiationSignals) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        if (negotiationSignals != null) {
            negotiationSignals.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(signals::add);
        }
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            signals.add("Processo alvo: " + processo.getNumeroUnificado());
        }
        if (processo.getRamoDireito() != null) {
            signals.add("Ramo: " + processo.getRamoDireito().name());
        }
        if (actor != null && actor.getTipoUsuario() != null) {
            signals.add("Ator emissor: " + actor.getTipoUsuario().name());
        }
        if (proposta != null && proposta.getStatus() != null) {
            signals.add("Status da proposta: " + proposta.getStatus().name());
        }
        if (recentChat != null && !recentChat.isEmpty()) {
            signals.add("Volume recente de chat: " + recentChat.size());
            long tensionMessages = recentChat.stream()
                    .filter(Objects::nonNull)
                    .map(ChatMensagem::getConteudo)
                    .filter(Objects::nonNull)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(this::containsTensionMarker)
                    .count();
            if (tensionMessages > 0) {
                signals.add("Há sinais textuais de tensão na conversa recente: " + tensionMessages);
            }
        }
        if (outboundMessage != null && !outboundMessage.isBlank()) {
            String normalized = outboundMessage.trim();
            signals.add("Mensagem candidata em governança: " + normalized);
            if (containsNumericProposal(normalized)) {
                signals.add("A mensagem candidata contém valor, percentual ou marcador econômico explícito.");
            }
            if (containsTensionMarker(normalized.toLowerCase(Locale.ROOT))) {
                signals.add("A mensagem candidata contém marcador textual de sensibilidade negocial.");
            }
        }
        return List.copyOf(signals);
    }

    private List<String> buildStrategicFocus(NegotiationPreflightBundle bundle,
                                             String outboundMessage,
                                             Usuario actor,
                                             List<ChatMensagem> recentChat) {
        LinkedHashSet<String> focus = new LinkedHashSet<>();
        if (bundle.settlementAdvisory() != null) {
            addAllSafe(focus, bundle.settlementAdvisory().nextMoves());
            addAllSafe(focus, bundle.settlementAdvisory().executionSafeguards());
        }
        if (bundle.institutionalGovernanceContext() != null) {
            addAllSafe(focus, bundle.institutionalGovernanceContext().policyGuards());
            addAllSafe(focus, bundle.institutionalGovernanceContext().governanceAlerts());
            addAllSafe(focus, bundle.institutionalGovernanceContext().escalationPlaybooks());
        }
        if (bundle.negotiationMemory() != null) {
            addAllSafe(focus, bundle.negotiationMemory().reusablePlaybooks());
            addAllSafe(focus, bundle.negotiationMemory().cautionPoints());
        }
        if (bundle.negotiationExplainability() != null) {
            addAllSafe(focus, bundle.negotiationExplainability().openQuestions());
            if (bundle.negotiationExplainability().nodes() != null) {
                bundle.negotiationExplainability().nodes().stream()
                        .map(NegotiationExplainabilityReport.NegotiationNode::title)
                        .filter(Objects::nonNull)
                        .forEach(focus::add);
            }
        }
        if (bundle.kernelOperationalGovernance() != null) {
            addAllSafe(focus, bundle.kernelOperationalGovernance().controls());
            addAllSafe(focus, bundle.kernelOperationalGovernance().watchpoints());
            addAllSafe(focus, bundle.kernelOperationalGovernance().nextActions());
        }
        if (bundle.negotiationChatDigest() != null) {
            focus.add(bundle.negotiationChatDigest().conversationStage());
            focus.add(bundle.negotiationChatDigest().posture());
            focus.add(bundle.negotiationChatDigest().temperature());
            focus.add(bundle.negotiationChatDigest().sendMode());
            addAllSafe(focus, bundle.negotiationChatDigest().nextTurnObjectives());
            addAllSafe(focus, bundle.negotiationChatDigest().forbiddenMoves());
            addAllSafe(focus, bundle.negotiationChatDigest().internalActions());
            addAllSafe(focus, bundle.negotiationChatDigest().messageBlueprints());
        }
        if (bundle.negotiationApprovalMatrix() != null) {
            focus.add(bundle.negotiationApprovalMatrix().approvalBand());
            focus.add(bundle.negotiationApprovalMatrix().releaseMode());
            addAllSafe(focus, bundle.negotiationApprovalMatrix().approvalGates());
            addAllSafe(focus, bundle.negotiationApprovalMatrix().escalationLanes());
            addAllSafe(focus, bundle.negotiationApprovalMatrix().internalControls());
            addAllSafe(focus, bundle.negotiationApprovalMatrix().releaseChecklist());
        }
        if (bundle.negotiationChannelGovernance() != null) {
            focus.add(bundle.negotiationChannelGovernance().operatingMode());
            focus.add(bundle.negotiationChannelGovernance().persistenceMode());
            focus.add(bundle.negotiationChannelGovernance().approvalHandshake());
            addAllSafe(focus, bundle.negotiationChannelGovernance().participantDirectives());
            addAllSafe(focus, bundle.negotiationChannelGovernance().releaseBoundaries());
            addAllSafe(focus, bundle.negotiationChannelGovernance().auditDirectives());
            addAllSafe(focus, bundle.negotiationChannelGovernance().memoryDirectives());
            addAllSafe(focus, bundle.negotiationChannelGovernance().deliveryGuardrails());
            addAllSafe(focus, bundle.negotiationChannelGovernance().fallbackLanes());
        }
        if (bundle.institutionalPolicySnapshot() != null) {
            focus.add(bundle.institutionalPolicySnapshot().policyKey());
            focus.add(bundle.institutionalPolicySnapshot().policyTier());
            focus.add(bundle.institutionalPolicySnapshot().policyVersion());
            if (bundle.institutionalPolicySnapshot().policyAxes() != null) {
                focus.add(bundle.institutionalPolicySnapshot().policyAxes().selectionMode());
                addAllSafe(focus, bundle.institutionalPolicySnapshot().policyAxes().matchedAxes());
                addAllSafe(focus, bundle.institutionalPolicySnapshot().policyAxes().declaredAxes());
            }
            addAllSafe(focus, bundle.institutionalPolicySnapshot().mandatoryDirectives());
            addAllSafe(focus, bundle.institutionalPolicySnapshot().blockingDirectives());
            addAllSafe(focus, bundle.institutionalPolicySnapshot().releaseGuardrails());
            addAllSafe(focus, bundle.institutionalPolicySnapshot().escalationTriggers());
        }
        if (bundle.kernelDecisionMetrics() != null) {
            addAllSafe(focus, bundle.kernelDecisionMetrics().hotSignals());
            addAllSafe(focus, bundle.kernelDecisionMetrics().stabilitySignals());
        }
        if (bundle.kernelRiskEscalation() != null) {
            focus.add(bundle.kernelRiskEscalation().escalationLevel());
            addAllSafe(focus, bundle.kernelRiskEscalation().containmentActions());
            addAllSafe(focus, bundle.kernelRiskEscalation().escalationTriggers());
            addAllSafe(focus, bundle.kernelRiskEscalation().recommendedLanes());
        }
        addAllSafe(focus, bundle.decision().reasons());
        addAllSafe(focus, bundle.decision().mandatoryActions());
        focus.add(bundle.decision().releaseMessage());
        if (actor != null && actor.getId() != null) {
            focus.add("Ator de envio governado: " + actor.getId());
        }
        if (recentChat != null && !recentChat.isEmpty()) {
            focus.add("Base governada de chat considerada: " + recentChat.size() + " mensagens.");
        }
        if (outboundMessage != null && !outboundMessage.isBlank()) {
            focus.add("Mensagem candidata auditada: " + trim(outboundMessage, 280));
        }
        focus.removeIf(value -> value == null || value.isBlank());
        return List.copyOf(focus);
    }

    private String resolvePersistedSuggestedMessage(NegotiationPreflightBundle bundle, String outboundMessage) {
        if (bundle.decision().releaseAllowed()) {
            return trim(outboundMessage, 2000);
        }
        String digestSuggestion = bundle.negotiationChatDigest() != null ? bundle.negotiationChatDigest().suggestedNextMessage() : null;
        if (digestSuggestion != null && !digestSuggestion.isBlank()) {
            return trim("Rascunho interno sugerido: " + digestSuggestion, 2000);
        }
        return trim("Rascunho interno obrigatório: " + bundle.decision().releaseMessage(), 2000);
    }


    private void addAllSafe(LinkedHashSet<String> target, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(target::add);
    }
    private boolean containsNumericProposal(String value) {
        return value.chars().anyMatch(Character::isDigit) || value.contains("%") || value.toLowerCase(Locale.ROOT).contains("r$");
    }

    private boolean containsTensionMarker(String value) {
        return value.contains("urgente")
                || value.contains("impasse")
                || value.contains("sigilo")
                || value.contains("confidencial")
                || value.contains("multa")
                || value.contains("inadimpl")
                || value.contains("bloqueio")
                || value.contains("prazo final");
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max);
    }
}
