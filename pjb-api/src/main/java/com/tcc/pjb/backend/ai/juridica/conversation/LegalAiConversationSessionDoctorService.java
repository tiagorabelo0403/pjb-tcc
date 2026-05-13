package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
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
public class LegalAiConversationSessionDoctorService {

    public LegalAiConversationSessionDoctorSnapshot inspect(LegalAiConversationRequest request,
                                                            String capability,
                                                            String version,
                                                            LegalAiConversationMemorySnapshot memory,
                                                            LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                            LegalAiConversationToolScopeSnapshot toolScope,
                                                            LegalValidationResponse validation,
                                                            LegalHallucinationGuardResponse guard) {
        Map<String, Object> diagnosticsSource = toolScope == null || toolScope.diagnostics() == null ? Map.of() : ImmutableViewSupport.map(toolScope.diagnostics());
        List<String> retainedApprovals = distinctTurnValues(memory, "approvalStatus");
        List<String> retainedHallucination = distinctTurnValues(memory, "hallucinationStatus");
        List<String> retainedSymbolic = distinctTurnValues(memory, "symbolicExecutionStatus");
        int contradictionPressure = countTurnsWithEvidence(memory, "contradictions");
        int missingEvidencePressure = countTurnsWithEvidence(memory, "missingEvidence");
        boolean replayReady = booleanValue(diagnosticsSource.get("mcpTranscriptReplayReady"));
        boolean benchmarkPassed = booleanValue(diagnosticsSource.get("mcpBenchmarkPassed"));
        boolean doctorReady = booleanValue(diagnosticsSource.get("mcpDoctorReady"));
        String doctorStatus = stringValue(diagnosticsSource.get("mcpDoctorStatus"));
        String evidenceStatus = stringValue(diagnosticsSource.get("mcpEvidencePromotionStatus"));
        Double qualityScore = numberValue(diagnosticsSource.get("mcpQualityScore"));
        List<String> skillIds = listValue(diagnosticsSource.get("mcpSkillIds"));
        List<String> toolExampleIds = listValue(diagnosticsSource.get("mcpToolExampleIds"));
        List<String> promotedToolExampleIds = listValue(diagnosticsSource.get("mcpPromotedToolExampleIds"));
        int retainedTurnCount = memory == null || memory.retainedTurns() == null ? 0 : memory.retainedTurns().size();
        boolean approvalDrift = retainedApprovals.size() > 1;
        boolean hallucinationDrift = retainedHallucination.size() > 1;
        boolean symbolicDrift = retainedSymbolic.size() > 1;
        boolean currentContradictions = validation != null && validation.contradictions() != null && !validation.contradictions().isEmpty();
        boolean currentMissingEvidence = validation != null && validation.missingEvidence() != null && !validation.missingEvidence().isEmpty();
        boolean currentHallucinationBlocked = guard != null && "BLOCKED".equalsIgnoreCase(guard.status());
        boolean documentRestricted = documentSecurity != null && !Objects.equals(documentSecurity.status(), "CLEARED");
        boolean replayDrift = retainedTurnCount >= 2 && (!replayReady || !benchmarkPassed || !doctorReady || "PROMOTION_HELD".equalsIgnoreCase(evidenceStatus));
        boolean driftDetected = approvalDrift || hallucinationDrift || symbolicDrift || replayDrift || contradictionPressure >= 2 || missingEvidencePressure >= 2 || currentContradictions || currentMissingEvidence;

        List<String> reasons = new ArrayList<>();
        if (approvalDrift) {
            reasons.add("Approval status oscilou nos turnos retidos da sessão.");
        }
        if (hallucinationDrift) {
            reasons.add("Grounding da sessão oscilou entre turnos retidos.");
        }
        if (symbolicDrift) {
            reasons.add("Execução simbólica não permaneceu estável ao longo da sessão.");
        }
        if (replayDrift) {
            reasons.add("Replay, benchmark ou doctor do MCP não estão estáveis para reaproveitamento seguro nesta sessão.");
        }
        if (contradictionPressure >= 2 || currentContradictions) {
            reasons.add("A sessão acumulou contradições suficientes para congelar reutilização automática.");
        }
        if (missingEvidencePressure >= 2 || currentMissingEvidence) {
            reasons.add("A sessão acumulou insuficiência probatória ou normativa acima do tolerável.");
        }
        if (documentRestricted) {
            reasons.add("A surface já está sob fence documental e não pode promover reuse livre nesta sessão.");
        }

        String status = resolveStatus(doctorStatus, currentHallucinationBlocked, documentRestricted, driftDetected, contradictionPressure, missingEvidencePressure, currentContradictions, currentMissingEvidence);
        boolean blockedSurface = "BLOCKED".equals(status);
        String operationalMode = blockedSurface
                ? "SESSION_LOCKDOWN"
                : "DEGRADED".equals(status)
                ? "SESSION_MONITORED_REPLAY_FROZEN"
                : "SESSION_READY";

        LinkedHashSet<String> blockedSkillIds = new LinkedHashSet<>();
        LinkedHashSet<String> blockedToolExampleIds = new LinkedHashSet<>();
        if (blockedSurface || replayDrift || approvalDrift || hallucinationDrift) {
            blockedSkillIds.addAll(skillIds);
        }
        if (blockedSurface) {
            blockedToolExampleIds.addAll(toolExampleIds);
            blockedToolExampleIds.addAll(promotedToolExampleIds);
        } else if (driftDetected || !"PROMOTED_FROM_REPLAY".equalsIgnoreCase(evidenceStatus)) {
            blockedToolExampleIds.addAll(promotedToolExampleIds.isEmpty() ? toolExampleIds : promotedToolExampleIds);
        }

        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("userProfile", request == null ? null : request.userProfile());
        diagnostics.put("processoId", request == null ? null : request.processoId());
        diagnostics.put("retainedTurnCount", retainedTurnCount);
        diagnostics.put("approvalDrift", approvalDrift);
        diagnostics.put("hallucinationDrift", hallucinationDrift);
        diagnostics.put("symbolicDrift", symbolicDrift);
        diagnostics.put("replayDrift", replayDrift);
        diagnostics.put("contradictionPressure", contradictionPressure);
        diagnostics.put("missingEvidencePressure", missingEvidencePressure);
        diagnostics.put("mcpDoctorStatus", doctorStatus);
        diagnostics.put("mcpDoctorReady", doctorReady);
        diagnostics.put("mcpBenchmarkPassed", benchmarkPassed);
        diagnostics.put("mcpTranscriptReplayReady", replayReady);
        diagnostics.put("mcpEvidencePromotionStatus", evidenceStatus);
        diagnostics.put("mcpQualityScore", qualityScore);
        diagnostics.put("retainedApprovalStatuses", retainedApprovals);
        diagnostics.put("retainedHallucinationStatuses", retainedHallucination);
        diagnostics.put("retainedSymbolicStatuses", retainedSymbolic);
        diagnostics.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        return new LegalAiConversationSessionDoctorSnapshot(
                status,
                blockedSurface,
                driftDetected,
                operationalMode,
                List.copyOf(blockedSkillIds),
                List.copyOf(blockedToolExampleIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private String resolveStatus(String doctorStatus,
                                 boolean currentHallucinationBlocked,
                                 boolean documentRestricted,
                                 boolean driftDetected,
                                 int contradictionPressure,
                                 int missingEvidencePressure,
                                 boolean currentContradictions,
                                 boolean currentMissingEvidence) {
        if ("BLOCKED".equalsIgnoreCase(doctorStatus) || currentHallucinationBlocked) {
            return "BLOCKED";
        }
        if (driftDetected && (documentRestricted || contradictionPressure >= 2 || missingEvidencePressure >= 2 || currentContradictions || currentMissingEvidence)) {
            return "BLOCKED";
        }
        if ("DEGRADED".equalsIgnoreCase(doctorStatus) || driftDetected || documentRestricted) {
            return "DEGRADED";
        }
        return "READY";
    }

    private List<String> distinctTurnValues(LegalAiConversationMemorySnapshot memory, String key) {
        if (memory == null || memory.retainedTurns() == null || key == null) {
            return List.of();
        }
        return memory.retainedTurns().stream()
                .filter(Objects::nonNull)
                .map(turn -> turn.get(key))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    @SuppressWarnings("unchecked")
    private int countTurnsWithEvidence(LegalAiConversationMemorySnapshot memory, String key) {
        if (memory == null || memory.retainedTurns() == null || key == null) {
            return 0;
        }
        return (int) memory.retainedTurns().stream()
                .filter(Objects::nonNull)
                .map(turn -> turn.get(key))
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .filter(list -> !list.isEmpty())
                .count();
    }

    @SuppressWarnings("unchecked")
    private List<String> listValue(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }
}
