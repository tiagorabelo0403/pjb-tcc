package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecoverySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRehabilitationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationCapabilityRehabilitationService {

    public LegalAiConversationCapabilityRehabilitationSnapshot inspect(LegalAiConversationRequest request,
                                                                       String capability,
                                                                       String version,
                                                                       LegalAiConversationMemorySnapshot memory,
                                                                       LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                       LegalAiConversationToolScopeSnapshot toolScope,
                                                                       LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                       LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                       LegalAiConversationCapabilityRecoverySnapshot capabilityRecovery,
                                                                       LegalAiConversationCapabilityCooldownSnapshot capabilityCooldown) {
        boolean recoveryRelevant = capabilityRecovery != null
                && (capabilityRecovery.capabilityRecovered()
                || capabilityRecovery.recoveryEligible()
                || (capabilityRecovery.recoveryCandidateToolIds() != null && !capabilityRecovery.recoveryCandidateToolIds().isEmpty()));
        boolean cooldownRelevant = capabilityCooldown != null
                && (capabilityCooldown.lockActive() || "MONITORED".equalsIgnoreCase(capabilityCooldown.status()));
        if (!recoveryRelevant && !cooldownRelevant) {
            return new LegalAiConversationCapabilityRehabilitationSnapshot(
                    "NOT_REQUIRED",
                    false,
                    false,
                    false,
                    "NONE",
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of(
                            "capability", capability,
                            "version", version,
                            "recoveryRelevant", false,
                            "cooldownRelevant", false
                    )
            );
        }
        int stableWinningTurns = countStableWinningTurns(memory, capability);
        int requiredStableTurns = cooldownRelevant ? 3 : 2;
        boolean cooldownLocked = capabilityCooldown != null && capabilityCooldown.lockActive();
        boolean documentCleared = documentSecurity != null && Objects.equals(documentSecurity.status(), "CLEARED");
        boolean sessionDoctorReady = sessionDoctor != null && "READY".equalsIgnoreCase(sessionDoctor.status()) && !sessionDoctor.driftDetected();
        boolean bootstrapReady = sessionBootstrap != null && !sessionBootstrap.blockedCapability() && !sessionBootstrap.repeatedDriftDetected();
        boolean profileRecoverable = sessionBootstrap == null || !"BLOCKED".equalsIgnoreCase(normalize(sessionBootstrap.profileGate()));
        boolean sigiloRecoverable = sessionBootstrap == null || !"BLOCKED".equalsIgnoreCase(normalize(sessionBootstrap.sigiloFence()));
        boolean recoveryRecovered = capabilityRecovery != null && capabilityRecovery.capabilityRecovered();
        boolean releaseEligible = !cooldownLocked && recoveryRecovered && documentCleared && sessionDoctorReady && bootstrapReady && profileRecoverable && sigiloRecoverable;
        List<String> releasedToolIds = capabilityRecovery == null || capabilityRecovery.recoveryCandidateToolIds() == null
                ? List.of()
                : List.copyOf(new LinkedHashSet<>(capabilityRecovery.recoveryCandidateToolIds()));
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>();
        if (capabilityCooldown != null && capabilityCooldown.blockedToolIds() != null) {
            blockedToolIds.addAll(capabilityCooldown.blockedToolIds());
        }
        if (blockedToolIds.isEmpty() && !releasedToolIds.isEmpty()) {
            blockedToolIds.addAll(releasedToolIds);
        }
        if (blockedToolIds.isEmpty() && toolScope != null && toolScope.stepUpToolIds() != null) {
            blockedToolIds.addAll(toolScope.stepUpToolIds());
        }
        List<String> unmetRequirements = new ArrayList<>();
        if (cooldownLocked) {
            unmetRequirements.add("CAPABILITY_COOLDOWN_LOCK_ACTIVE");
        }
        if (!recoveryRecovered) {
            unmetRequirements.add("CAPABILITY_RECOVERY_NOT_RELEASED");
        }
        if (!documentCleared) {
            unmetRequirements.add("DOCUMENT_SECURITY_NOT_CLEARED");
        }
        if (!sessionDoctorReady) {
            unmetRequirements.add("SESSION_DOCTOR_NOT_READY");
        }
        if (!bootstrapReady) {
            unmetRequirements.add("SESSION_BOOTSTRAP_NOT_READY");
        }
        if (!profileRecoverable) {
            unmetRequirements.add("PROFILE_GATE_BLOCKED");
        }
        if (!sigiloRecoverable) {
            unmetRequirements.add("SIGILO_FENCE_BLOCKED");
        }
        if (stableWinningTurns < requiredStableTurns) {
            unmetRequirements.add("STABILITY_WINDOW_INCOMPLETE");
        }
        boolean capabilityReleased = releaseEligible && stableWinningTurns >= requiredStableTurns;
        int rehabilitationWindowTurnsRemaining = capabilityReleased ? 0 : Math.max(0, requiredStableTurns - stableWinningTurns);
        String releaseLane;
        String status;
        if (!recoveryRecovered || !profileRecoverable || !sigiloRecoverable) {
            status = "BLOCKED";
            releaseLane = "REHABILITATION_DENIED";
        } else if (capabilityReleased) {
            status = "RELEASED";
            releaseLane = requiresStepUpLane(capability, request == null ? null : request.message(), sessionBootstrap == null ? null : sessionBootstrap.sigiloFence())
                    ? "REHABILITATION_STEP_UP_GATED"
                    : "REHABILITATION_MONITORED_RELEASE";
        } else {
            status = stableWinningTurns == 0 ? "COUNTING" : "MONITORED";
            releaseLane = "REHABILITATION_STABILITY_WINDOW";
        }
        List<String> reasons = new ArrayList<>();
        if (capabilityReleased) {
            reasons.add("A capability entrou em janela formal de reabilitação e liberou a volta após acumular estabilidade vencedora suficiente por turno.");
        } else {
            reasons.add("A capability permanece em janela formal de reabilitação até encerrar a contagem de estabilidade vencedora por turno.");
        }
        if (rehabilitationWindowTurnsRemaining > 0) {
            reasons.add("Ainda faltam " + rehabilitationWindowTurnsRemaining + " turno(s) estável(is) para liberar a capability sem reabrir a oscilação operacional.");
        }
        if (!unmetRequirements.isEmpty()) {
            reasons.add("Requisitos pendentes: " + String.join(", ", unmetRequirements) + '.');
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("processoId", request == null ? null : request.processoId());
        diagnostics.put("userProfile", request == null ? null : request.userProfile());
        diagnostics.put("stableWinningTurns", stableWinningTurns);
        diagnostics.put("requiredStableTurns", requiredStableTurns);
        diagnostics.put("rehabilitationWindowTurnsRemaining", rehabilitationWindowTurnsRemaining);
        diagnostics.put("releaseEligible", releaseEligible);
        diagnostics.put("capabilityReleased", capabilityReleased);
        diagnostics.put("releaseLane", releaseLane);
        diagnostics.put("recoveryRecovered", recoveryRecovered);
        diagnostics.put("cooldownLocked", cooldownLocked);
        diagnostics.put("documentCleared", documentCleared);
        diagnostics.put("sessionDoctorReady", sessionDoctorReady);
        diagnostics.put("bootstrapReady", bootstrapReady);
        diagnostics.put("profileRecoverable", profileRecoverable);
        diagnostics.put("sigiloRecoverable", sigiloRecoverable);
        diagnostics.put("releasedToolIds", releasedToolIds);
        diagnostics.put("blockedToolIds", List.copyOf(blockedToolIds));
        return new LegalAiConversationCapabilityRehabilitationSnapshot(
                status,
                true,
                releaseEligible,
                capabilityReleased,
                releaseLane,
                stableWinningTurns,
                requiredStableTurns,
                rehabilitationWindowTurnsRemaining,
                releasedToolIds,
                List.copyOf(blockedToolIds),
                List.copyOf(unmetRequirements),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private int countStableWinningTurns(LegalAiConversationMemorySnapshot memory, String capability) {
        if (memory == null || memory.retainedTurns() == null || memory.retainedTurns().isEmpty()) {
            return 0;
        }
        int count = 0;
        List<Map<String, Object>> turns = memory.retainedTurns();
        for (int i = turns.size() - 1; i >= 0; i--) {
            Map<String, Object> turn = safeTurn(turns.get(i));
            if (!sameCapability(turn, capability)) {
                if (count > 0) {
                    break;
                }
                continue;
            }
            if (!isStableWinningTurn(turn)) {
                break;
            }
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeTurn(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private boolean sameCapability(Map<String, Object> turn, String capability) {
        String turnCapability = normalize(turn == null ? null : turn.get("capability"));
        String effectiveCapability = normalize(capability);
        return effectiveCapability != null && effectiveCapability.equals(turnCapability);
    }

    private boolean isStableWinningTurn(Map<String, Object> turn) {
        String approvalStatus = normalize(turn.get("approvalStatus"));
        String hallucinationStatus = normalize(turn.get("hallucinationStatus"));
        String symbolicStatus = normalize(turn.get("symbolicExecutionStatus"));
        boolean contradictions = listSize(turn.get("contradictions")) > 0;
        boolean missingEvidence = listSize(turn.get("missingEvidence")) > 0;
        return !"HUMAN_REVIEW_REQUIRED".equals(approvalStatus)
                && !"STEP_UP_REQUIRED".equals(approvalStatus)
                && !"READONLY_RESTRICTED".equals(approvalStatus)
                && !"BLOCKED".equals(hallucinationStatus)
                && !"BLOCKED".equals(symbolicStatus)
                && !contradictions
                && !missingEvidence;
    }

    private int listSize(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    private boolean requiresStepUpLane(String capability, String message, String sigiloFence) {
        if ("DEGRADED".equals(normalize(sigiloFence))) {
            return true;
        }
        String normalized = normalize((capability == null ? "" : capability) + ' ' + (message == null ? "" : message));
        if (normalized == null) {
            return false;
        }
        return normalized.contains("PETIC")
                || normalized.contains("PROTOCOLO")
                || normalized.contains("RECURS")
                || normalized.contains("MINUTA")
                || normalized.contains("PARECER")
                || normalized.contains("DECIS")
                || normalized.contains("SENTENC")
                || normalized.contains("VOTO");
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text.toUpperCase(Locale.ROOT);
    }
}
