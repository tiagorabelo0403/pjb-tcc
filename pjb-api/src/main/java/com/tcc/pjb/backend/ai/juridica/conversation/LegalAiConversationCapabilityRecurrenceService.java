package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecoverySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRehabilitationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecurrenceSnapshot;
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
public class LegalAiConversationCapabilityRecurrenceService {

    public LegalAiConversationCapabilityRecurrenceSnapshot inspect(LegalAiConversationRequest request,
                                                                   String capability,
                                                                   String version,
                                                                   LegalAiConversationMemorySnapshot memory,
                                                                   LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                   LegalAiConversationToolScopeSnapshot toolScope,
                                                                   LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                   LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                   LegalAiConversationCapabilityRecoverySnapshot capabilityRecovery,
                                                                   LegalAiConversationCapabilityCooldownSnapshot capabilityCooldown,
                                                                   LegalAiConversationCapabilityRehabilitationSnapshot capabilityRehabilitation) {
        String normalizedCapability = normalize(capability);
        String normalizedProcessoId = normalize(request == null ? null : request.processoId());
        String normalizedConversationId = normalize(request == null ? null : request.conversationId());
        boolean processScoped = normalizedProcessoId != null;
        String registryScope = processScoped ? "SESSION_PROCESS_CAPABILITY" : "SESSION_CAPABILITY";
        String registryKey = (normalizedConversationId == null ? "NO_CONVERSATION" : normalizedConversationId)
                + '|'
                + (normalizedProcessoId == null ? "NO_PROCESS" : normalizedProcessoId)
                + '|'
                + (normalizedCapability == null ? "NO_CAPABILITY" : normalizedCapability);
        int recurrenceCount = countRecurrenceIncidents(memory, normalizedCapability, normalizedProcessoId);
        boolean repeatedDriftDetected = (sessionDoctor != null && sessionDoctor.driftDetected())
                || (sessionBootstrap != null && sessionBootstrap.repeatedDriftDetected());
        boolean cooldownLocked = capabilityCooldown != null && capabilityCooldown.lockActive();
        boolean recoveryRecovered = capabilityRecovery != null && capabilityRecovery.capabilityRecovered();
        boolean rehabilitationBlocked = capabilityRehabilitation != null && "BLOCKED".equalsIgnoreCase(capabilityRehabilitation.status());
        boolean rehabilitationWindowOpen = capabilityRehabilitation != null
                && capabilityRehabilitation.rehabilitationRequired()
                && !capabilityRehabilitation.capabilityReleased();
        int failedRehabilitationCount = calculateFailedRehabilitationCount(recurrenceCount, repeatedDriftDetected, cooldownLocked, rehabilitationBlocked, rehabilitationWindowOpen, recoveryRecovered);
        int quarantineHitCount = documentSecurity != null && !"CLEARED".equalsIgnoreCase(documentSecurity.status()) ? 1 : 0;
        boolean processRecidivism = processScoped && (recurrenceCount >= 3 || failedRehabilitationCount >= 2 || repeatedDriftDetected);
        boolean recurrenceDetected = recurrenceCount > 0 || failedRehabilitationCount > 0 || repeatedDriftDetected || cooldownLocked || rehabilitationBlocked || rehabilitationWindowOpen;
        String riskTier = riskTier(recurrenceCount, failedRehabilitationCount, repeatedDriftDetected, quarantineHitCount, cooldownLocked, rehabilitationBlocked, processRecidivism);
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>();
        if (capabilityCooldown != null && capabilityCooldown.blockedToolIds() != null) {
            blockedToolIds.addAll(capabilityCooldown.blockedToolIds());
        }
        if (capabilityRehabilitation != null && capabilityRehabilitation.blockedToolIds() != null) {
            blockedToolIds.addAll(capabilityRehabilitation.blockedToolIds());
        }
        if (blockedToolIds.isEmpty() && capabilityRehabilitation != null && capabilityRehabilitation.releasedToolIds() != null) {
            blockedToolIds.addAll(capabilityRehabilitation.releasedToolIds());
        }
        if (blockedToolIds.isEmpty() && toolScope != null && toolScope.stepUpToolIds() != null) {
            blockedToolIds.addAll(toolScope.stepUpToolIds());
        }
        List<String> unmetRequirements = new ArrayList<>();
        if (processRecidivism) {
            unmetRequirements.add("PROCESS_RECIDIVISM_THRESHOLD_REACHED");
        }
        if (failedRehabilitationCount >= 2) {
            unmetRequirements.add("REHABILITATION_REINCIDENCE_DETECTED");
        }
        if (repeatedDriftDetected) {
            unmetRequirements.add("REPEATED_DRIFT_DETECTED");
        }
        if (quarantineHitCount > 0) {
            unmetRequirements.add("DOCUMENT_SECURITY_NOT_CLEARED");
        }
        if (cooldownLocked) {
            unmetRequirements.add("CAPABILITY_COOLDOWN_LOCK_ACTIVE");
        }
        if (rehabilitationBlocked) {
            unmetRequirements.add("CAPABILITY_REHABILITATION_BLOCKED");
        }
        if (rehabilitationWindowOpen) {
            unmetRequirements.add("CAPABILITY_REHABILITATION_WINDOW_OPEN");
        }
        String status;
        String escalationMode;
        if (!recurrenceDetected) {
            status = "NOT_REQUIRED";
            escalationMode = "NONE";
        } else if ("CRITICAL".equals(riskTier) || (processRecidivism && (cooldownLocked || rehabilitationBlocked || repeatedDriftDetected))) {
            status = "LOCKED";
            escalationMode = processScoped ? "PROCESS_SCOPED_HARD_LOCK" : "SESSION_HARD_LOCK";
        } else if ("HIGH".equals(riskTier) || processRecidivism) {
            status = "ESCALATED";
            escalationMode = quarantineHitCount > 0 || rehabilitationBlocked ? "PROCESS_SCOPED_HUMAN_REVIEW" : "PROCESS_SCOPED_STEP_UP";
        } else {
            status = "MONITORED";
            escalationMode = processScoped ? "PROCESS_SCOPED_MONITORING" : "SESSION_MONITORING";
        }
        List<String> reasons = new ArrayList<>();
        if (!recurrenceDetected) {
            reasons.add("Nenhuma reincidência operacional relevante foi detectada para esta capability na janela retida da sessão.");
        } else {
            reasons.add("A capability entrou em registry de reincidência porque a mesma trilha voltou a oscilar na mesma sessão" + (processScoped ? " e no mesmo processo." : "."));
            if (processRecidivism) {
                reasons.add("A reincidência por capability e processo atingiu limiar suficiente para endurecer a governança antes de nova liberação automática.");
            }
            if (failedRehabilitationCount > 0) {
                reasons.add("A janela de reabilitação já mostrou reincidência suficiente para impedir reabertura ingênua da capability.");
            }
            if (repeatedDriftDetected) {
                reasons.add("O doctor contínuo e o bootstrap de sessão apontaram deriva repetida, elevando a necessidade de contenção.");
            }
            if (cooldownLocked) {
                reasons.add("O cooldown lock segue ativo e mantém a capability congelada para evitar abre-fecha instável.");
            }
            if (rehabilitationBlocked) {
                reasons.add("A reabilitação material da capability ainda falhou em convergir para estado seguro.");
            }
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("registryScope", registryScope);
        diagnostics.put("registryKey", registryKey);
        diagnostics.put("processoId", request == null ? null : request.processoId());
        diagnostics.put("userProfile", request == null ? null : request.userProfile());
        diagnostics.put("retainedTurnCount", memory == null || memory.retainedTurns() == null ? 0 : memory.retainedTurns().size());
        diagnostics.put("recurrenceCount", recurrenceCount);
        diagnostics.put("failedRehabilitationCount", failedRehabilitationCount);
        diagnostics.put("repeatedDriftDetected", repeatedDriftDetected);
        diagnostics.put("quarantineHitCount", quarantineHitCount);
        diagnostics.put("processRecidivism", processRecidivism);
        diagnostics.put("riskTier", riskTier);
        diagnostics.put("cooldownLocked", cooldownLocked);
        diagnostics.put("rehabilitationBlocked", rehabilitationBlocked);
        diagnostics.put("rehabilitationWindowOpen", rehabilitationWindowOpen);
        diagnostics.put("capabilityRecoveryRecovered", recoveryRecovered);
        diagnostics.put("blockedToolIds", List.copyOf(blockedToolIds));
        diagnostics.put("status", status);
        diagnostics.put("escalationMode", escalationMode);
        return new LegalAiConversationCapabilityRecurrenceSnapshot(
                status,
                recurrenceDetected,
                processScoped,
                registryKey,
                recurrenceCount,
                failedRehabilitationCount,
                repeatedDriftDetected,
                quarantineHitCount,
                riskTier,
                escalationMode,
                List.copyOf(blockedToolIds),
                List.copyOf(unmetRequirements),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private int countRecurrenceIncidents(LegalAiConversationMemorySnapshot memory, String capability, String processoId) {
        if (memory == null || memory.retainedTurns() == null || memory.retainedTurns().isEmpty() || capability == null) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> turn : memory.retainedTurns()) {
            if (!sameCapability(turn, capability) || !sameProcess(turn, processoId)) {
                continue;
            }
            if (isRiskIncident(turn)) {
                count++;
            }
        }
        return count;
    }

    private boolean sameCapability(Map<String, Object> turn, String capability) {
        return Objects.equals(normalize(turn == null ? null : turn.get("capability")), capability);
    }

    private boolean sameProcess(Map<String, Object> turn, String processoId) {
        if (processoId == null) {
            return true;
        }
        return Objects.equals(normalize(turn == null ? null : turn.get("processoId")), processoId);
    }

    private boolean isRiskIncident(Map<String, Object> turn) {
        String approvalStatus = normalize(turn == null ? null : turn.get("approvalStatus"));
        String hallucinationStatus = normalize(turn == null ? null : turn.get("hallucinationStatus"));
        String symbolicStatus = normalize(turn == null ? null : turn.get("symbolicExecutionStatus"));
        return "HUMAN_REVIEW_REQUIRED".equals(approvalStatus)
                || "STEP_UP_REQUIRED".equals(approvalStatus)
                || "READONLY_RESTRICTED".equals(approvalStatus)
                || "BLOCKED".equals(hallucinationStatus)
                || "BLOCKED".equals(symbolicStatus)
                || listSize(turn == null ? null : turn.get("contradictions")) > 0
                || listSize(turn == null ? null : turn.get("missingEvidence")) > 0;
    }

    private int calculateFailedRehabilitationCount(int recurrenceCount,
                                                   boolean repeatedDriftDetected,
                                                   boolean cooldownLocked,
                                                   boolean rehabilitationBlocked,
                                                   boolean rehabilitationWindowOpen,
                                                   boolean recoveryRecovered) {
        int count = 0;
        if (rehabilitationBlocked) {
            count = count + 2;
        }
        if (rehabilitationWindowOpen && !recoveryRecovered) {
            count++;
        }
        if (cooldownLocked) {
            count++;
        }
        if (repeatedDriftDetected) {
            count++;
        }
        if (recurrenceCount >= 4) {
            count++;
        }
        return count;
    }

    private String riskTier(int recurrenceCount,
                            int failedRehabilitationCount,
                            boolean repeatedDriftDetected,
                            int quarantineHitCount,
                            boolean cooldownLocked,
                            boolean rehabilitationBlocked,
                            boolean processRecidivism) {
        if (rehabilitationBlocked || cooldownLocked || (processRecidivism && repeatedDriftDetected) || failedRehabilitationCount >= 3) {
            return "CRITICAL";
        }
        if (recurrenceCount >= 4 || failedRehabilitationCount >= 2 || repeatedDriftDetected || quarantineHitCount > 0) {
            return "HIGH";
        }
        if (recurrenceCount >= 2 || failedRehabilitationCount > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private int listSize(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text.toUpperCase(Locale.ROOT);
    }
}
