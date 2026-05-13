package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecoverySnapshot;
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
public class LegalAiConversationCapabilityCooldownService {

    public LegalAiConversationCapabilityCooldownSnapshot inspect(LegalAiConversationRequest request,
                                                                 String capability,
                                                                 String version,
                                                                 LegalAiConversationMemorySnapshot memory,
                                                                 LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                 LegalAiConversationToolScopeSnapshot toolScope,
                                                                 LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                 LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                 LegalAiConversationCapabilityRecoverySnapshot capabilityRecovery) {
        boolean recoveryRelevant = capabilityRecovery != null
                && (capabilityRecovery.capabilityRecovered()
                || capabilityRecovery.recoveryEligible()
                || (capabilityRecovery.recoveryCandidateToolIds() != null && !capabilityRecovery.recoveryCandidateToolIds().isEmpty()));
        boolean bootstrapBlocked = sessionBootstrap != null && sessionBootstrap.blockedCapability();
        if (!recoveryRelevant && !bootstrapBlocked) {
            return new LegalAiConversationCapabilityCooldownSnapshot(
                    "NOT_REQUIRED",
                    false,
                    request != null && request.processoId() != null && !request.processoId().isBlank() ? "SESSION_PROCESS" : "SESSION_ONLY",
                    lockKey(request, capability),
                    0,
                    false,
                    List.of(),
                    List.of(),
                    Map.of(
                            "capability", capability,
                            "version", version,
                            "recoveryRelevant", false,
                            "bootstrapBlocked", bootstrapBlocked
                    )
            );
        }
        int retainedTurnCount = memory == null || memory.retainedTurns() == null ? 0 : memory.retainedTurns().size();
        int instabilityCount = countInstability(memory, capability);
        int stableTailCount = countStableTail(memory, capability);
        boolean repeatedDrift = instabilityCount >= 2
                || (sessionDoctor != null && sessionDoctor.driftDetected())
                || (sessionBootstrap != null && sessionBootstrap.repeatedDriftDetected());
        boolean doctorStable = sessionDoctor != null && "READY".equalsIgnoreCase(sessionDoctor.status()) && !sessionDoctor.driftDetected();
        boolean documentCleared = documentSecurity != null && Objects.equals(documentSecurity.status(), "CLEARED");
        boolean pendingRecovery = capabilityRecovery != null && "PENDING".equalsIgnoreCase(capabilityRecovery.status());
        boolean recovered = capabilityRecovery != null && capabilityRecovery.capabilityRecovered();
        boolean flapRisk = (recovered && instabilityCount >= 2) || (pendingRecovery && instabilityCount >= 3);
        boolean lockActive = repeatedDrift && (flapRisk || !doctorStable || !documentCleared);
        int cooldownTurnsRemaining = lockActive ? Math.max(1, 3 - stableTailCount) : 0;
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>();
        if (capabilityRecovery != null && capabilityRecovery.recoveryCandidateToolIds() != null) {
            blockedToolIds.addAll(capabilityRecovery.recoveryCandidateToolIds());
        }
        if (blockedToolIds.isEmpty() && toolScope != null) {
            if (toolScope.stepUpToolIds() != null) {
                blockedToolIds.addAll(toolScope.stepUpToolIds());
            }
            if (blockedToolIds.isEmpty() && toolScope.allowedToolIds() != null) {
                blockedToolIds.addAll(toolScope.allowedToolIds());
            }
        }
        List<String> reasons = new ArrayList<>();
        if (lockActive) {
            reasons.add("Capability entrou em cooldown para impedir abre-fecha instável após drift recorrente na mesma sessão ou no mesmo processo.");
        } else if (repeatedDrift) {
            reasons.add("Capability ficou em cooldown monitorado porque o histórico recente ainda mostra sinais de instabilidade operacional.");
        }
        if (!documentCleared) {
            reasons.add("Fence documental ainda não está limpa para reabrir a capability com segurança plena.");
        }
        if (!doctorStable) {
            reasons.add("Doctor de sessão ainda não estabilizou o fluxo desta capability.");
        }
        if (instabilityCount > 0) {
            reasons.add("Histórico recente registrou " + instabilityCount + " turno(s) instável(is) para a mesma capability.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("lockScope", request != null && request.processoId() != null && !request.processoId().isBlank() ? "SESSION_PROCESS" : "SESSION_ONLY");
        diagnostics.put("lockKey", lockKey(request, capability));
        diagnostics.put("retainedTurnCount", retainedTurnCount);
        diagnostics.put("instabilityCount", instabilityCount);
        diagnostics.put("stableTailCount", stableTailCount);
        diagnostics.put("repeatedDrift", repeatedDrift);
        diagnostics.put("doctorStable", doctorStable);
        diagnostics.put("documentCleared", documentCleared);
        diagnostics.put("pendingRecovery", pendingRecovery);
        diagnostics.put("capabilityRecovered", recovered);
        diagnostics.put("blockedToolIds", List.copyOf(blockedToolIds));
        return new LegalAiConversationCapabilityCooldownSnapshot(
                lockActive ? "LOCKED" : repeatedDrift ? "MONITORED" : "CLEAR",
                lockActive,
                String.valueOf(diagnostics.get("lockScope")),
                String.valueOf(diagnostics.get("lockKey")),
                cooldownTurnsRemaining,
                lockActive,
                List.copyOf(blockedToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private int countInstability(LegalAiConversationMemorySnapshot memory, String capability) {
        if (memory == null || memory.retainedTurns() == null || memory.retainedTurns().isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = memory.retainedTurns().size() - 1; i >= 0 && count < 4; i--) {
            Map<String, Object> turn = safeTurn(memory.retainedTurns().get(i));
            if (!sameCapability(turn, capability)) {
                continue;
            }
            if (isUnstable(turn)) {
                count++;
            }
        }
        return count;
    }

    private int countStableTail(LegalAiConversationMemorySnapshot memory, String capability) {
        if (memory == null || memory.retainedTurns() == null || memory.retainedTurns().isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = memory.retainedTurns().size() - 1; i >= 0 && count < 3; i--) {
            Map<String, Object> turn = safeTurn(memory.retainedTurns().get(i));
            if (!sameCapability(turn, capability)) {
                continue;
            }
            if (isUnstable(turn)) {
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

    private boolean isUnstable(Map<String, Object> turn) {
        String approvalStatus = normalize(turn.get("approvalStatus"));
        String hallucinationStatus = normalize(turn.get("hallucinationStatus"));
        String symbolicStatus = normalize(turn.get("symbolicExecutionStatus"));
        boolean contradictions = listSize(turn.get("contradictions")) > 0;
        boolean missingEvidence = listSize(turn.get("missingEvidence")) > 0;
        return "HUMAN_REVIEW_REQUIRED".equals(approvalStatus)
                || "STEP_UP_REQUIRED".equals(approvalStatus)
                || "READONLY_RESTRICTED".equals(approvalStatus)
                || "BLOCKED".equals(hallucinationStatus)
                || "BLOCKED".equals(symbolicStatus)
                || contradictions
                || missingEvidence;
    }

    private int listSize(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    private String lockKey(LegalAiConversationRequest request, String capability) {
        String conversationId = request == null || request.conversationId() == null || request.conversationId().isBlank()
                ? "anonymous-session"
                : request.conversationId().trim();
        String processoId = request == null || request.processoId() == null || request.processoId().isBlank()
                ? "no-process"
                : request.processoId().trim();
        String effectiveCapability = capability == null || capability.isBlank() ? "unknown-capability" : capability.trim();
        return conversationId + '|' + processoId + '|' + effectiveCapability;
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.toUpperCase(Locale.ROOT);
    }
}
