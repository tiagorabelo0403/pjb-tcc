package com.tcc.pjb.backend.ai.juridica.conversation;

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
public class LegalAiConversationCapabilityRecoveryService {

    public LegalAiConversationCapabilityRecoverySnapshot inspect(LegalAiConversationRequest request,
                                                                 String capability,
                                                                 String version,
                                                                 LegalAiConversationMemorySnapshot memory,
                                                                 LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                 LegalAiConversationToolScopeSnapshot toolScope,
                                                                 LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                 LegalAiConversationSessionBootstrapSnapshot sessionBootstrap) {
        if (sessionBootstrap == null || !sessionBootstrap.blockedCapability()) {
            return new LegalAiConversationCapabilityRecoverySnapshot(
                    "NOT_REQUIRED",
                    false,
                    false,
                    "NONE",
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of(
                            "capability", capability,
                            "version", version,
                            "blockedByBootstrap", false
                    )
            );
        }
        Map<String, Object> diagnosticsSource = toolScope == null || toolScope.diagnostics() == null ? Map.of() : ImmutableViewSupport.map(toolScope.diagnostics());
        List<String> recoveryCandidateToolIds = listValue(diagnosticsSource.get("sessionBootstrapRecoveryCandidateToolIds"));
        boolean replayReady = booleanValue(diagnosticsSource.get("mcpTranscriptReplayReady"));
        boolean benchmarkPassed = booleanValue(diagnosticsSource.get("mcpBenchmarkPassed"));
        boolean doctorReady = booleanValue(diagnosticsSource.get("mcpDoctorReady"));
        String evidenceStatus = stringValue(diagnosticsSource.get("mcpEvidencePromotionStatus"));
        String doctorStatus = stringValue(diagnosticsSource.get("mcpDoctorStatus"));
        String profileGate = normalize(sessionBootstrap.profileGate());
        String sigiloFence = normalize(sessionBootstrap.sigiloFence());
        boolean documentCleared = documentSecurity != null && Objects.equals(documentSecurity.status(), "CLEARED");
        boolean sessionDoctorStable = sessionDoctor != null && "READY".equalsIgnoreCase(sessionDoctor.status()) && !sessionDoctor.driftDetected();
        boolean coverageReady = (sessionBootstrap.missingSkillIds() == null || sessionBootstrap.missingSkillIds().isEmpty())
                && (sessionBootstrap.missingToolExampleIds() == null || sessionBootstrap.missingToolExampleIds().isEmpty());
        boolean profileRecoverable = !"BLOCKED".equals(profileGate);
        boolean sigiloRecoverable = !"BLOCKED".equals(sigiloFence);
        boolean evidenceReady = "PROMOTED_FROM_REPLAY".equalsIgnoreCase(evidenceStatus) || "RECOVERY_CONFIRMED".equalsIgnoreCase(evidenceStatus);
        boolean recoveryEligible = profileRecoverable && sigiloRecoverable;
        List<String> unmetRequirements = new ArrayList<>();
        if (!replayReady) {
            unmetRequirements.add("REPLAY_NOT_READY");
        }
        if (!benchmarkPassed) {
            unmetRequirements.add("BENCHMARK_NOT_PASSED");
        }
        if (!doctorReady || "BLOCKED".equalsIgnoreCase(doctorStatus)) {
            unmetRequirements.add("MCP_DOCTOR_UNSTABLE");
        }
        if (!sessionDoctorStable) {
            unmetRequirements.add("SESSION_DOCTOR_UNSTABLE");
        }
        if (!coverageReady) {
            unmetRequirements.add("SKILL_EXAMPLE_COVERAGE_INCOMPLETE");
        }
        if (!documentCleared) {
            unmetRequirements.add("DOCUMENT_SECURITY_NOT_CLEARED");
        }
        if (!evidenceReady) {
            unmetRequirements.add("EVIDENCE_PROMOTION_NOT_READY");
        }
        if (!profileRecoverable) {
            unmetRequirements.add("PROFILE_GATE_BLOCKED");
        }
        if (!sigiloRecoverable) {
            unmetRequirements.add("SIGILO_FENCE_BLOCKED");
        }
        boolean capabilityRecovered = recoveryEligible
                && replayReady
                && benchmarkPassed
                && doctorReady
                && sessionDoctorStable
                && coverageReady
                && documentCleared
                && evidenceReady;
        String status;
        String recoveryLane;
        if (!recoveryEligible) {
            status = "DENIED";
            recoveryLane = "LOCKED_BY_PROFILE_OR_SIGILO";
        } else if (capabilityRecovered) {
            status = "RECOVERED";
            recoveryLane = requestStepUpRecovery(capability, request == null ? null : request.message(), sigiloFence)
                    ? "RECOVERY_STEP_UP_MONITORED"
                    : "RECOVERY_MONITORED";
        } else {
            status = "PENDING";
            recoveryLane = "RECOVERY_WAITING_EVIDENCE";
        }
        List<String> reasons = new ArrayList<>();
        if (!profileRecoverable) {
            reasons.add("O perfil jurídico atual permanece incompatível com a capability bloqueada.");
        }
        if (!sigiloRecoverable) {
            reasons.add("A fence de sigilo ainda impede reabertura segura da capability nesta sessão.");
        }
        if (capabilityRecovered) {
            reasons.add("A capability foi reaberta porque replay, benchmark, doctor, cobertura mínima e fence documental convergiram para estado seguro.");
        } else if (recoveryEligible) {
            reasons.add("A capability continua bloqueada até convergência entre replay vencedor, doctor estável e recomposição mínima de skills/examples.");
        }
        if (!unmetRequirements.isEmpty()) {
            reasons.add("Requisitos ainda pendentes: " + String.join(", ", unmetRequirements) + '.');
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("userProfile", request == null ? null : request.userProfile());
        diagnostics.put("processoId", request == null ? null : request.processoId());
        diagnostics.put("retainedTurnCount", memory == null || memory.retainedTurns() == null ? 0 : memory.retainedTurns().size());
        diagnostics.put("profileGate", sessionBootstrap.profileGate());
        diagnostics.put("sigiloFence", sessionBootstrap.sigiloFence());
        diagnostics.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        diagnostics.put("replayReady", replayReady);
        diagnostics.put("benchmarkPassed", benchmarkPassed);
        diagnostics.put("mcpDoctorReady", doctorReady);
        diagnostics.put("mcpDoctorStatus", doctorStatus);
        diagnostics.put("evidencePromotionStatus", evidenceStatus);
        diagnostics.put("sessionDoctorStatus", sessionDoctor == null ? null : sessionDoctor.status());
        diagnostics.put("sessionDoctorDriftDetected", sessionDoctor != null && sessionDoctor.driftDetected());
        diagnostics.put("coverageReady", coverageReady);
        diagnostics.put("recoveryEligible", recoveryEligible);
        diagnostics.put("recoveryCandidateToolIds", recoveryCandidateToolIds);
        return new LegalAiConversationCapabilityRecoverySnapshot(
                status,
                recoveryEligible,
                capabilityRecovered,
                recoveryLane,
                List.copyOf(new LinkedHashSet<>(recoveryCandidateToolIds)),
                List.copyOf(unmetRequirements),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private boolean requestStepUpRecovery(String capability, String message, String sigiloFence) {
        if ("DEGRADED".equals(sigiloFence)) {
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

    @SuppressWarnings("unchecked")
    private List<String> listValue(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
