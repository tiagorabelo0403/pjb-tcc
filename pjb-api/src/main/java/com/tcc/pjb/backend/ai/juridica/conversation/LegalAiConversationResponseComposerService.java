package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSanitizationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTraceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiSchemaDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationResponseComposerService {

    public String compose(LegalAiConversationRequest request,
                          String capability,
                          String version,
                          String councilHeadline,
                          List<Map<String, Object>> council,
                          LegalValidationResponse validation,
                          LegalHallucinationGuardResponse guard,
                          LegalAiConversationApprovalSnapshot approval,
                          LegalAiConversationTraceSnapshot trace,
                          LegalAiConversationMemorySnapshot memory,
                          LegalAiConversationSanitizationSnapshot sanitization,
                          LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                          LegalAiConversationToolScopeSnapshot toolScope,
                          LegalAiSchemaDefinition recommendedSchema) {
        List<String> lines = new ArrayList<>();
        lines.add(answerHeader(request, version));
        lines.add("Painel interno de raciocínio: " + councilHeadline + '.');
        lines.add(answerBody(capability, validation, guard, approval, sanitization, documentSecurity, toolScope));
        lines.add(schemaLine(recommendedSchema));
        lines.add(mcpLine(toolScope));
        lines.add(traceLine(trace, memory));
        lines.add(nextActionLine(council, approval, documentSecurity));
        return String.join("\n\n", lines);
    }

    public Map<String, Object> safeguards(LegalValidationResponse validation,
                                          LegalHallucinationGuardResponse guard,
                                          LegalAiConversationApprovalSnapshot approval,
                                          LegalAiConversationTraceSnapshot trace,
                                          LegalAiConversationMemorySnapshot memory,
                                          LegalAiConversationSanitizationSnapshot sanitization,
                                          LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                          LegalAiConversationToolScopeSnapshot toolScope,
                                          LegalAiSchemaDefinition recommendedSchema) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("hallucinationStatus", guard == null ? null : guard.status());
        out.put("hallucinationPlaceholder", guard == null ? null : guard.unresolvedCitationPlaceholder());
        out.put("citationMode", guard == null ? null : guard.citationEmissionMode());
        out.put("validationContradictions", validation == null ? List.of() : validation.contradictions());
        out.put("missingEvidence", validation == null ? List.of() : validation.missingEvidence());
        out.put("symbolicExecutionStatus", validation == null || validation.trace() == null ? null : validation.trace().get("symbolicExecutionStatus"));
        out.put("approvalRequired", approval != null && approval.approvalRequired());
        out.put("approvalStatus", approval == null ? null : approval.status());
        out.put("approvalCheckpoints", approval == null ? List.of() : approval.checkpoints());
        out.put("traceId", trace == null ? null : trace.traceId());
        out.put("turnId", trace == null ? null : trace.turnId());
        out.put("traceLane", trace == null ? null : trace.lane());
        out.put("memoryRetainedTurns", memory == null || memory.retainedTurns() == null ? 0 : memory.retainedTurns().size());
        out.put("memoryScopes", memory == null || memory.scopedMemory() == null ? List.of() : List.copyOf(memory.scopedMemory().keySet()));
        out.put("sanitizationStatus", sanitization == null ? null : sanitization.status());
        out.put("promptInjectionDetected", sanitization != null && sanitization.promptInjectionDetected());
        out.put("sanitizationAlerts", sanitization == null ? List.of() : sanitization.alerts());
        out.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        out.put("quarantinedAttachments", documentSecurity == null ? List.of() : documentSecurity.quarantinedAttachments());
        out.put("blockedSources", documentSecurity == null ? List.of() : documentSecurity.blockedSources());
        out.put("toolScopeStatus", toolScope == null ? null : toolScope.status());
        out.put("blockedToolIds", toolScope == null ? List.of() : toolScope.blockedToolIds());
        out.put("stepUpToolIds", toolScope == null ? List.of() : toolScope.stepUpToolIds());
        out.put("recommendedSchemaId", recommendedSchema == null ? null : recommendedSchema.schemaId());
        out.put("recommendedSchemaLabel", recommendedSchema == null ? null : recommendedSchema.label());
        out.put("recommendedSchemaStage", recommendedSchema == null ? null : recommendedSchema.stage());
        out.put("mcpSelectionMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpSelectionMode"));
        out.put("mcpPinnedServers", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("mcpPinnedServers", List.of()));
        out.put("mcpQualityScore", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpQualityScore"));
        out.put("mcpBenchmarkPassed", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpBenchmarkPassed"));
        out.put("mcpPromotionCandidates", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("mcpPromotionCandidates", List.of()));
        out.put("mcpDemotionCandidates", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("mcpDemotionCandidates", List.of()));
        out.put("mcpSkillIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("mcpSkillIds", List.of()));
        out.put("mcpToolExampleIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("mcpToolExampleIds", List.of()));
        out.put("mcpDeliberationMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpDeliberationMode"));
        out.put("mcpDeliberationRequired", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpDeliberationRequired"));
        out.put("mcpContextCompactionPolicy", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpContextCompactionPolicy"));
        out.put("mcpTranscriptId", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpTranscriptId"));
        out.put("mcpTranscriptMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpTranscriptMode"));
        out.put("mcpTranscriptReplayReady", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpTranscriptReplayReady"));
        out.put("mcpDoctorStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpDoctorStatus"));
        out.put("mcpDoctorReady", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpDoctorReady"));
        out.put("mcpDoctorOperationalMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpDoctorOperationalMode"));
        out.put("mcpEvidencePromotionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpEvidencePromotionStatus"));
        out.put("mcpPromotedToolExampleIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("mcpPromotedToolExampleIds", List.of()));
        out.put("mcpEvidenceApprovalLane", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpEvidenceApprovalLane"));
        out.put("capabilityCooldownStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityCooldownStatus"));
        out.put("capabilityCooldownLockActive", toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityCooldownLockActive")));
        out.put("capabilityCooldownLockScope", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityCooldownLockScope"));
        out.put("capabilityCooldownTurnsRemaining", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityCooldownTurnsRemaining", 0));
        out.put("capabilityCooldownBlockedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityCooldownBlockedToolIds", List.of()));
        out.put("capabilityRehabilitationStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRehabilitationStatus"));
        out.put("capabilityRehabilitationReleased", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRehabilitationReleased"));
        out.put("capabilityRehabilitationReleaseLane", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRehabilitationReleaseLane"));
        out.put("capabilityRehabilitationStableWinningTurns", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRehabilitationStableWinningTurns", 0));
        out.put("capabilityRehabilitationRequiredStableTurns", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRehabilitationRequiredStableTurns", 0));
        out.put("capabilityRehabilitationWindowTurnsRemaining", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRehabilitationWindowTurnsRemaining", 0));
        out.put("capabilityRehabilitationReleasedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRehabilitationReleasedToolIds", List.of()));
        out.put("capabilityRehabilitationBlockedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRehabilitationBlockedToolIds", List.of()));
        out.put("capabilityRehabilitationUnmetRequirements", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRehabilitationUnmetRequirements", List.of()));
        out.put("mcpEvidenceScore", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpEvidenceScore"));
        out.put("sessionDoctorStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionDoctorStatus"));
        out.put("sessionDoctorBlockedSurface", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionDoctorBlockedSurface"));
        out.put("sessionDoctorDriftDetected", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionDoctorDriftDetected"));
        out.put("sessionDoctorOperationalMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionDoctorOperationalMode"));
        out.put("sessionDoctorBlockedSkillIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("sessionDoctorBlockedSkillIds", List.of()));
        out.put("sessionDoctorBlockedToolExampleIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("sessionDoctorBlockedToolExampleIds", List.of()));
        out.put("sessionBootstrapStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionBootstrapStatus"));
        out.put("sessionBootstrapBlockedCapability", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionBootstrapBlockedCapability"));
        out.put("sessionBootstrapRepeatedDriftDetected", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionBootstrapRepeatedDriftDetected"));
        out.put("sessionBootstrapOperationalMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionBootstrapOperationalMode"));
        out.put("sessionBootstrapProfileGate", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionBootstrapProfileGate"));
        out.put("sessionBootstrapSigiloFence", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("sessionBootstrapSigiloFence"));
        out.put("sessionBootstrapMissingSkillIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("sessionBootstrapMissingSkillIds", List.of()));
        out.put("sessionBootstrapMissingToolExampleIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("sessionBootstrapMissingToolExampleIds", List.of()));
        out.put("capabilityRecoveryStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecoveryStatus"));
        out.put("capabilityRecoveryRecovered", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecoveryRecovered"));
        out.put("capabilityRecoveryLane", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecoveryLane"));
        out.put("capabilityRecoveryCandidateToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecoveryCandidateToolIds", List.of()));
        out.put("capabilityRecoveryUnmetRequirements", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecoveryUnmetRequirements", List.of()));
        out.put("capabilityRecurrenceStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceStatus"));
        out.put("capabilityRecurrenceDetected", toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityRecurrenceDetected")));
        out.put("capabilityRecurrenceRegistryKey", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceRegistryKey"));
        out.put("capabilityRecurrenceCount", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRecurrenceCount", 0));
        out.put("capabilityRecurrenceFailedRehabilitationCount", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRecurrenceFailedRehabilitationCount", 0));
        out.put("capabilityRecurrenceRiskTier", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceRiskTier"));
        out.put("capabilityRecurrenceEscalationMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceEscalationMode"));
        out.put("capabilityRecurrenceBlockedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecurrenceBlockedToolIds", List.of()));
        out.put("capabilityRecurrenceUnmetRequirements", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecurrenceUnmetRequirements", List.of()));
        out.put("capabilitySuppressionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionStatus"));
        out.put("capabilitySuppressionDetected", toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilitySuppressionDetected")));
        out.put("capabilitySuppressionMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionMode"));
        out.put("capabilitySuppressionPolicyTier", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionPolicyTier"));
        out.put("trustZoneStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneStatus"));
        out.put("trustZone", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZone"));
        out.put("trustZoneSovereignBoundaryRequired", toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("trustZoneSovereignBoundaryRequired")));
        out.put("trustZoneMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneMode"));
        out.put("trustZoneSourceZone", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneSourceZone"));
        out.put("trustZoneAttachmentZone", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneAttachmentZone"));
        out.put("trustZoneCapabilityZone", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneCapabilityZone"));
        out.put("evidenceProvenanceStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("evidenceProvenanceStatus"));
        out.put("evidenceProvenanceTier", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("evidenceProvenanceTier"));
        out.put("evidenceSourceTier", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("evidenceSourceTier"));
        out.put("evidenceAttachmentTier", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("evidenceAttachmentTier"));
        out.put("evidenceProvenanceMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("evidenceProvenanceMode"));
        out.put("ragPromotionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("ragPromotionStatus"));
        out.put("groundingPromotionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("groundingPromotionStatus"));
        out.put("draftPromotionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("draftPromotionStatus"));
        out.put("suggestionPromotionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("suggestionPromotionStatus"));
        out.put("capabilityRecoveryPromotionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecoveryPromotionStatus"));
        out.put("officialEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("officialEvidenceIds", List.of()));
        out.put("institutionalControlledEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("institutionalControlledEvidenceIds", List.of()));
        out.put("derivedEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("derivedEvidenceIds", List.of()));
        out.put("untrustedEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("untrustedEvidenceIds", List.of()));
        out.put("evidenceDescriptors", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("evidenceDescriptors", List.of()));
        out.put("promotedRagEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("promotedRagEvidenceIds", List.of()));
        out.put("promotedGroundingEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("promotedGroundingEvidenceIds", List.of()));
        out.put("promotedDraftEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("promotedDraftEvidenceIds", List.of()));
        out.put("capabilitySuppressionProcessClass", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionProcessClass"));
        out.put("capabilitySuppressionSigiloLevel", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionSigiloLevel"));
        out.put("capabilitySuppressionBlockedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilitySuppressionBlockedToolIds", List.of()));
        out.put("capabilitySuppressionStepUpToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilitySuppressionStepUpToolIds", List.of()));
        out.put("capabilitySuppressionUnmetRequirements", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilitySuppressionUnmetRequirements", List.of()));
        out.put("preConsciousStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("preConsciousStatus"));
        out.put("preConsciousMode", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("preConsciousMode"));
        out.put("preConsciousAuthorityFloor", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("preConsciousAuthorityFloor"));
        out.put("preConsciousCognitivePosture", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("preConsciousCognitivePosture"));
        out.put("preConsciousRiskScore", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("preConsciousRiskScore", 0));
        out.put("preConsciousHumanReviewRequired", toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("preConsciousHumanReviewRequired")));
        out.put("preConsciousLearningCandidate", toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("preConsciousLearningCandidate")));
        out.put("preConsciousLineages", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("preConsciousLineages", List.of()));
        out.put("preConsciousDominantLenses", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("preConsciousDominantLenses", List.of()));
        out.put("preConsciousAuthorityChecks", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("preConsciousAuthorityChecks", List.of()));
        out.put("preConsciousSignals", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("preConsciousSignals", List.of()));
        out.put("preConsciousNextActions", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("preConsciousNextActions", List.of()));
        return ImmutableViewSupport.map(out);
    }

    private String answerHeader(LegalAiConversationRequest request, String version) {
        String message = request == null || request.message() == null ? "consulta jurídica" : request.message().trim();
        return "Resposta conversacional jurídica " + version + ": analisando o seu pedido \"" + message + "\" com a malha unificada do PJB.";
    }

    private String answerBody(String capability,
                              LegalValidationResponse validation,
                              LegalHallucinationGuardResponse guard,
                              LegalAiConversationApprovalSnapshot approval,
                              LegalAiConversationSanitizationSnapshot sanitization,
                              LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                              LegalAiConversationToolScopeSnapshot toolScope) {
        StringBuilder out = new StringBuilder();
        out.append("Capability aplicada: ").append(capability).append('.');
        if (approval != null && approval.approvalRequired()) {
            out.append(" O turno foi mantido em modo ").append(approval.status()).append(" para impedir ação sensível fora da governança jurídica.");
        }
        if (documentSecurity != null && !"CLEARED".equalsIgnoreCase(documentSecurity.status())) {
            out.append(" O bloco documental ficou em ").append(documentSecurity.status()).append(" e anexos ou fontes fora da política não entram no contexto ampliado.");
        }
        if (toolScope != null && toolScope.blockedToolIds() != null && !toolScope.blockedToolIds().isEmpty()) {
            out.append(" A malha de ferramentas foi reduzida ao escopo seguro deste turno.");
        }
        String sessionDoctorStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("sessionDoctorStatus", ""));
        boolean sessionDoctorBlocked = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionDoctorBlockedSurface"));
        boolean sessionDoctorDrift = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionDoctorDriftDetected"));
        if (sessionDoctorBlocked) {
            out.append(" O doctor contínuo de sessão bloqueou a surface até replay vencedor e estabilização material do histórico retido.");
        } else if (sessionDoctorDrift || "DEGRADED".equalsIgnoreCase(sessionDoctorStatus)) {
            out.append(" O doctor contínuo de sessão congelou reuse de skills e examples até nova convergência do replay operacional.");
        }
        String sessionBootstrapStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("sessionBootstrapStatus", ""));
        boolean sessionBootstrapBlocked = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionBootstrapBlockedCapability"));
        boolean sessionBootstrapDrift = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionBootstrapRepeatedDriftDetected"));
        if (sessionBootstrapBlocked) {
            out.append(" O bootstrap contínuo de sessão bloqueou a capability até convergência entre perfil jurídico, sigilo e replay operacional.");
        } else if (sessionBootstrapDrift || "DEGRADED".equalsIgnoreCase(sessionBootstrapStatus)) {
            out.append(" O bootstrap contínuo de sessão elevou a capability para gate assistido por perfil, sigilo ou cobertura mínima ausente.");
        }
        String capabilityRecoveryStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecoveryStatus", ""));
        boolean capabilityRecoveryRecovered = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityRecoveryRecovered"));
        if (capabilityRecoveryRecovered) {
            out.append(" A capability recovery lane reabriu a capability em modo monitorado com step-up e replay vencedor confirmado.");
        } else if ("PENDING".equalsIgnoreCase(capabilityRecoveryStatus)) {
            out.append(" A capability recovery lane manteve a capability congelada enquanto aguarda replay vencedor, doctor estável e cobertura mínima recomposta.");
        } else if ("DENIED".equalsIgnoreCase(capabilityRecoveryStatus)) {
            out.append(" A capability recovery lane negou reabertura porque o bloqueio estrutural de perfil jurídico ou de sigilo permanece ativo.");
        }
        String capabilityCooldownStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityCooldownStatus", ""));
        boolean capabilityCooldownLocked = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityCooldownLockActive"));
        Object capabilityCooldownTurnsRemaining = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityCooldownTurnsRemaining");
        if (capabilityCooldownLocked) {
            out.append(" O capability cooldown lock congelou os recovery candidates para evitar abre-fecha instável e só admite reavaliação após janela mínima de estabilidade.");
        } else if ("MONITORED".equalsIgnoreCase(capabilityCooldownStatus)) {
            out.append(" O capability cooldown deixou a capability em monitoramento assistido para evitar reabertura prematura em trilha instável.");
        }
        String capabilityRehabilitationStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRehabilitationStatus", ""));
        boolean capabilityRehabilitationReleased = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityRehabilitationReleased"));
        Object capabilityRehabilitationStableWinningTurns = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRehabilitationStableWinningTurns");
        Object capabilityRehabilitationRequiredStableTurns = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRehabilitationRequiredStableTurns");
        if (capabilityRehabilitationReleased) {
            out.append(" A janela formal de reabilitação liberou a capability após acumular estabilidade vencedora por turno suficiente para reuso assistido.");
        } else if ("COUNTING".equalsIgnoreCase(capabilityRehabilitationStatus) || "MONITORED".equalsIgnoreCase(capabilityRehabilitationStatus)) {
            out.append(" A janela formal de reabilitação ainda está contando estabilidade vencedora por turno (")
                    .append(String.valueOf(capabilityRehabilitationStableWinningTurns))
                    .append('/')
                    .append(String.valueOf(capabilityRehabilitationRequiredStableTurns))
                    .append(") antes de liberar a capability novamente.");
        } else if ("BLOCKED".equalsIgnoreCase(capabilityRehabilitationStatus)) {
            out.append(" A janela formal de reabilitação negou a liberação da capability porque o bloqueio estrutural de recuperação, perfil ou sigilo ainda persiste.");
        }
        String capabilityRecurrenceStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecurrenceStatus", ""));
        boolean capabilityRecurrenceDetected = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityRecurrenceDetected"));
        Object capabilityRecurrenceCount = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceCount");
        Object capabilityRecurrenceRiskTier = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceRiskTier");
        Object capabilityRecurrenceEscalationMode = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceEscalationMode");
        if ("LOCKED".equalsIgnoreCase(capabilityRecurrenceStatus)) {
            out.append(" O registry de reincidência por capability/processo travou a trilha porque a mesma capability voltou a oscilar repetidamente no mesmo escopo operacional.");
        } else if ("ESCALATED".equalsIgnoreCase(capabilityRecurrenceStatus)) {
            out.append(" O registry de reincidência por capability/processo elevou a governança para ")
                    .append(String.valueOf(capabilityRecurrenceEscalationMode))
                    .append(" após detectar reincidência material (count=")
                    .append(String.valueOf(capabilityRecurrenceCount))
                    .append(", riskTier=")
                    .append(String.valueOf(capabilityRecurrenceRiskTier))
                    .append(").");
        } else if (capabilityRecurrenceDetected) {
            out.append(" O registry de reincidência por capability/processo entrou em monitoramento preventivo para evitar nova abertura instável desta capability.");
        }
        String capabilitySuppressionStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilitySuppressionStatus", ""));
        boolean capabilitySuppressionDetected = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilitySuppressionDetected"));
        Object capabilitySuppressionMode = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionMode");
        Object capabilitySuppressionProcessClass = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionProcessClass");
        Object capabilitySuppressionSigiloLevel = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilitySuppressionSigiloLevel");
        if ("LOCKED".equalsIgnoreCase(capabilitySuppressionStatus)) {
            out.append(" A supressão adaptativa por classe processual e sigilo travou a capability porque o ramo ")
                    .append(String.valueOf(capabilitySuppressionProcessClass))
                    .append(" com sigilo ")
                    .append(String.valueOf(capabilitySuppressionSigiloLevel))
                    .append(" exige contenção reforçada antes de qualquer reaproveitamento sensível.");
        } else if ("ESCALATED".equalsIgnoreCase(capabilitySuppressionStatus)) {
            out.append(" A supressão adaptativa por classe processual e sigilo elevou a capability para ")
                    .append(String.valueOf(capabilitySuppressionMode))
                    .append(" por se tratar de fluxo sensível no ramo ")
                    .append(String.valueOf(capabilitySuppressionProcessClass))
                    .append('.');
        } else if (capabilitySuppressionDetected) {
            out.append(" A supressão adaptativa por classe processual e sigilo manteve a capability em vigilância reforçada neste turno.");
        }
        String trustZoneStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZoneStatus", ""));
        String trustZone = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZone", ""));
        Object trustZoneMode = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneMode");
        Object trustZoneSourceZone = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneSourceZone");
        Object trustZoneAttachmentZone = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("trustZoneAttachmentZone");
        if ("LOCKED".equalsIgnoreCase(trustZoneStatus)) {
            out.append(" A trust zone soberana travou a capability porque fonte, anexo ou sigilo empurraram o turno para a zona ")
                    .append(String.valueOf(trustZone))
                    .append(" em modo ")
                    .append(String.valueOf(trustZoneMode))
                    .append('.');
        } else if ("ESCALATED".equalsIgnoreCase(trustZoneStatus) || "ENFORCED".equalsIgnoreCase(trustZoneStatus)) {
            out.append(" A trust zone soberana manteve a trilha em ")
                    .append(String.valueOf(trustZone))
                    .append(" com fronteira ")
                    .append(String.valueOf(trustZoneSourceZone))
                    .append('/')
                    .append(String.valueOf(trustZoneAttachmentZone))
                    .append(" antes de qualquer reuse automático sensível.");
        }
        String evidenceProvenanceStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("evidenceProvenanceStatus", ""));
        Object evidenceProvenanceTier = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("evidenceProvenanceTier");
        Object evidenceProvenanceMode = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("evidenceProvenanceMode");
        Object ragPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("ragPromotionStatus");
        Object groundingPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("groundingPromotionStatus");
        Object draftPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("draftPromotionStatus");
        Object suggestionPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("suggestionPromotionStatus");
        Object capabilityRecoveryPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecoveryPromotionStatus");
        if ("LOCKED".equalsIgnoreCase(evidenceProvenanceStatus)) {
            out.append(" O registry soberano de proveniência travou a promoção de evidência porque a cadeia efetiva caiu em ")
                    .append(String.valueOf(evidenceProvenanceTier))
                    .append(" no modo ")
                    .append(String.valueOf(evidenceProvenanceMode))
                    .append(", bloqueando grounding, RAG, minuta e recovery lane.");
        } else if ("ESCALATED".equalsIgnoreCase(evidenceProvenanceStatus) || "ENFORCED".equalsIgnoreCase(evidenceProvenanceStatus)) {
            out.append(" O registry soberano de proveniência manteve a promotion em ")
                    .append(String.valueOf(evidenceProvenanceTier))
                    .append(" com RAG=")
                    .append(String.valueOf(ragPromotionStatus))
                    .append(", grounding=")
                    .append(String.valueOf(groundingPromotionStatus))
                    .append(", minuta=")
                    .append(String.valueOf(draftPromotionStatus))
                    .append(", suggestionFlow=")
                    .append(String.valueOf(suggestionPromotionStatus))
                    .append(" e capabilityRecovery=")
                    .append(String.valueOf(capabilityRecoveryPromotionStatus))
                    .append('.');
        }
        String preConsciousStatus = toolScope == null || toolScope.diagnostics() == null ? "" : String.valueOf(toolScope.diagnostics().getOrDefault("preConsciousStatus", ""));
        Object preConsciousMode = toolScope == null || toolScope.diagnostics() == null ? "" : toolScope.diagnostics().getOrDefault("preConsciousMode", "");
        Object preConsciousAuthorityFloor = toolScope == null || toolScope.diagnostics() == null ? "" : toolScope.diagnostics().getOrDefault("preConsciousAuthorityFloor", "");
        Object preConsciousRiskScore = toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("preConsciousRiskScore", 0);
        Object preConsciousLineages = toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("preConsciousLineages", List.of());
        if ("BLOCKED".equalsIgnoreCase(preConsciousStatus)) {
            out.append(" A moldura pré-consciente jurídica bloqueou o turno em ")
                    .append(String.valueOf(preConsciousMode))
                    .append(" com piso ")
                    .append(String.valueOf(preConsciousAuthorityFloor))
                    .append(" e risco ")
                    .append(String.valueOf(preConsciousRiskScore))
                    .append("/100.");
        } else if ("ESCALATED".equalsIgnoreCase(preConsciousStatus)) {
            out.append(" A moldura pré-consciente jurídica elevou o turno para revisão assistida, usando linhagem jurídica ")
                    .append(String.valueOf(preConsciousLineages))
                    .append(" e piso ")
                    .append(String.valueOf(preConsciousAuthorityFloor))
                    .append(".");
        }
        if (sanitization != null && sanitization.promptInjectionDetected()) {
            out.append(" Marcadores de prompt injection foram neutralizados e encaminhados para revisão material.");
        }
        if (guard != null && "BLOCKED".equalsIgnoreCase(guard.status())) {
            out.append(" A resposta foi colocada em modo restrito porque o grounding não confirmou a base normativa ou jurisprudencial necessária.");
        } else if (guard != null) {
            out.append(" A resposta foi mantida em modo grounding-first e não emitirá artigo, precedente ou súmula sem confirmação.");
        }
        if (validation != null && validation.contradictions() != null && !validation.contradictions().isEmpty()) {
            out.append(" Há contradições a revisar antes de tratar isso como conclusão fechada: ").append(String.join("; ", validation.contradictions())).append('.');
        }
        if (validation != null && validation.missingEvidence() != null && !validation.missingEvidence().isEmpty()) {
            out.append(" Faltam elementos para robustecer a orientação: ").append(String.join("; ", validation.missingEvidence())).append('.');
        }
        return out.toString();
    }

    private String mcpLine(LegalAiConversationToolScopeSnapshot toolScope) {
        if (toolScope == null || toolScope.diagnostics() == null) {
            return "Plano MCP inteligente indisponível para este turno.";
        }
        Object selectionMode = toolScope.diagnostics().get("mcpSelectionMode");
        Object pinnedServers = toolScope.diagnostics().get("mcpPinnedServers");
        Object qualityScore = toolScope.diagnostics().get("mcpQualityScore");
        Object benchmarkPassed = toolScope.diagnostics().get("mcpBenchmarkPassed");
        Object skillIds = toolScope.diagnostics().get("mcpSkillIds");
        Object toolExampleIds = toolScope.diagnostics().get("mcpToolExampleIds");
        Object deliberationMode = toolScope.diagnostics().get("mcpDeliberationMode");
        Object contextCompaction = toolScope.diagnostics().get("mcpContextCompactionPolicy");
        Object transcriptMode = toolScope.diagnostics().get("mcpTranscriptMode");
        Object doctorStatus = toolScope.diagnostics().get("mcpDoctorStatus");
        Object promotedExamples = toolScope.diagnostics().get("mcpPromotedToolExampleIds");
        Object evidenceLane = toolScope.diagnostics().get("mcpEvidenceApprovalLane");
        Object evidenceStatus = toolScope.diagnostics().get("mcpEvidencePromotionStatus");
        Object sessionDoctorStatus = toolScope.diagnostics().get("sessionDoctorStatus");
        Object sessionDoctorMode = toolScope.diagnostics().get("sessionDoctorOperationalMode");
        Object sessionBootstrapStatus = toolScope.diagnostics().get("sessionBootstrapStatus");
        Object sessionBootstrapMode = toolScope.diagnostics().get("sessionBootstrapOperationalMode");
        Object sessionBootstrapProfileGate = toolScope.diagnostics().get("sessionBootstrapProfileGate");
        Object sessionBootstrapSigiloFence = toolScope.diagnostics().get("sessionBootstrapSigiloFence");
        Object capabilityRecoveryStatus = toolScope.diagnostics().get("capabilityRecoveryStatus");
        Object capabilityRecoveryLane = toolScope.diagnostics().get("capabilityRecoveryLane");
        Object capabilityRecoveryRecovered = toolScope.diagnostics().get("capabilityRecoveryRecovered");
        Object capabilityCooldownStatus = toolScope.diagnostics().get("capabilityCooldownStatus");
        Object capabilityCooldownScope = toolScope.diagnostics().get("capabilityCooldownLockScope");
        Object capabilityCooldownLocked = toolScope.diagnostics().get("capabilityCooldownLockActive");
        Object capabilityCooldownTurnsRemaining = toolScope.diagnostics().get("capabilityCooldownTurnsRemaining");
        Object capabilityRehabilitationStatus = toolScope.diagnostics().get("capabilityRehabilitationStatus");
        Object capabilityRehabilitationReleased = toolScope.diagnostics().get("capabilityRehabilitationReleased");
        Object capabilityRehabilitationReleaseLane = toolScope.diagnostics().get("capabilityRehabilitationReleaseLane");
        Object capabilityRehabilitationStableWinningTurns = toolScope.diagnostics().get("capabilityRehabilitationStableWinningTurns");
        Object capabilityRehabilitationRequiredStableTurns = toolScope.diagnostics().get("capabilityRehabilitationRequiredStableTurns");
        Object capabilityRehabilitationWindowTurnsRemaining = toolScope.diagnostics().get("capabilityRehabilitationWindowTurnsRemaining");
        Object capabilityRecurrenceStatus = toolScope.diagnostics().get("capabilityRecurrenceStatus");
        Object capabilityRecurrenceCount = toolScope.diagnostics().get("capabilityRecurrenceCount");
        Object capabilityRecurrenceRiskTier = toolScope.diagnostics().get("capabilityRecurrenceRiskTier");
        Object capabilityRecurrenceEscalationMode = toolScope.diagnostics().get("capabilityRecurrenceEscalationMode");
        Object trustZoneStatus = toolScope.diagnostics().get("trustZoneStatus");
        Object trustZone = toolScope.diagnostics().get("trustZone");
        Object trustZoneMode = toolScope.diagnostics().get("trustZoneMode");
        Object trustZoneSourceZone = toolScope.diagnostics().get("trustZoneSourceZone");
        Object trustZoneAttachmentZone = toolScope.diagnostics().get("trustZoneAttachmentZone");
        Object evidenceProvenanceStatus = toolScope.diagnostics().get("evidenceProvenanceStatus");
        Object evidenceProvenanceTier = toolScope.diagnostics().get("evidenceProvenanceTier");
        Object evidenceProvenanceMode = toolScope.diagnostics().get("evidenceProvenanceMode");
        Object ragPromotionStatus = toolScope.diagnostics().get("ragPromotionStatus");
        Object groundingPromotionStatus = toolScope.diagnostics().get("groundingPromotionStatus");
        Object draftPromotionStatus = toolScope.diagnostics().get("draftPromotionStatus");
        Object preConsciousStatus = toolScope.diagnostics().get("preConsciousStatus");
        Object preConsciousMode = toolScope.diagnostics().get("preConsciousMode");
        Object preConsciousAuthorityFloor = toolScope.diagnostics().get("preConsciousAuthorityFloor");
        Object preConsciousRiskScore = toolScope.diagnostics().get("preConsciousRiskScore");
        return "Plano MCP inteligente: modo " + String.valueOf(selectionMode) + ", servidores fixados " + String.valueOf(pinnedServers) + ", skills " + String.valueOf(skillIds) + ", examples " + String.valueOf(toolExampleIds) + ", examples promovidos " + String.valueOf(promotedExamples) + ", lane de evidência " + String.valueOf(evidenceLane) + ", status de promoção " + String.valueOf(evidenceStatus) + ", checkpoint " + String.valueOf(deliberationMode) + ", compactação " + String.valueOf(contextCompaction) + ", transcript " + String.valueOf(transcriptMode) + ", doctor " + String.valueOf(doctorStatus) + ", sessionDoctor " + String.valueOf(sessionDoctorStatus) + "/" + String.valueOf(sessionDoctorMode) + ", sessionBootstrap " + String.valueOf(sessionBootstrapStatus) + "/" + String.valueOf(sessionBootstrapMode) + " (profileGate=" + String.valueOf(sessionBootstrapProfileGate) + ", sigiloFence=" + String.valueOf(sessionBootstrapSigiloFence) + "), capabilityRecovery=" + String.valueOf(capabilityRecoveryStatus) + "/" + String.valueOf(capabilityRecoveryLane) + "/recovered=" + String.valueOf(capabilityRecoveryRecovered) + ", capabilityCooldown=" + String.valueOf(capabilityCooldownStatus) + "/scope=" + String.valueOf(capabilityCooldownScope) + "/locked=" + String.valueOf(capabilityCooldownLocked) + "/turnsRemaining=" + String.valueOf(capabilityCooldownTurnsRemaining) + ", capabilityRehabilitation=" + String.valueOf(capabilityRehabilitationStatus) + "/released=" + String.valueOf(capabilityRehabilitationReleased) + "/lane=" + String.valueOf(capabilityRehabilitationReleaseLane) + "/stable=" + String.valueOf(capabilityRehabilitationStableWinningTurns) + "/" + String.valueOf(capabilityRehabilitationRequiredStableTurns) + "/windowRemaining=" + String.valueOf(capabilityRehabilitationWindowTurnsRemaining) + ", capabilityRecurrence=" + String.valueOf(capabilityRecurrenceStatus) + "/count=" + String.valueOf(capabilityRecurrenceCount) + "/riskTier=" + String.valueOf(capabilityRecurrenceRiskTier) + "/mode=" + String.valueOf(capabilityRecurrenceEscalationMode) + ", trustZone=" + String.valueOf(trustZoneStatus) + "/" + String.valueOf(trustZone) + "/" + String.valueOf(trustZoneMode) + " source=" + String.valueOf(trustZoneSourceZone) + " attachment=" + String.valueOf(trustZoneAttachmentZone) + ", evidenceProvenance=" + String.valueOf(evidenceProvenanceStatus) + "/" + String.valueOf(evidenceProvenanceTier) + "/" + String.valueOf(evidenceProvenanceMode) + " rag=" + String.valueOf(ragPromotionStatus) + " grounding=" + String.valueOf(groundingPromotionStatus) + " draft=" + String.valueOf(draftPromotionStatus) + ", preConscious=" + String.valueOf(preConsciousStatus) + "/" + String.valueOf(preConsciousMode) + "/floor=" + String.valueOf(preConsciousAuthorityFloor) + "/risk=" + String.valueOf(preConsciousRiskScore) + ", score de benchmark " + String.valueOf(qualityScore) + " e benchmarkPassed=" + String.valueOf(benchmarkPassed) + ".";
    }

    private String schemaLine(LegalAiSchemaDefinition recommendedSchema) {
        if (recommendedSchema == null) {
            return "Schema estruturado recomendado indisponível para este turno.";
        }
        return "Schema estruturado recomendado: " + recommendedSchema.label() + " (" + recommendedSchema.schemaId() + ") na etapa " + recommendedSchema.stage() + ".";
    }

    private String traceLine(LegalAiConversationTraceSnapshot trace, LegalAiConversationMemorySnapshot memory) {
        if (trace == null) {
            return "Trace conversacional indisponível.";
        }
        int retainedTurns = memory == null || memory.retainedTurns() == null ? 0 : memory.retainedTurns().size();
        return "Trace do turno: lane " + trace.lane() + ", traceId " + trace.traceId() + ", turnId " + trace.turnId() + ", memória retida em " + retainedTurns + " turno(s).";
    }

    private String nextActionLine(List<Map<String, Object>> council,
                                  LegalAiConversationApprovalSnapshot approval,
                                  LegalAiConversationDocumentSecuritySnapshot documentSecurity) {
        if (documentSecurity != null && documentSecurity.alerts() != null && !documentSecurity.alerts().isEmpty()) {
            return "Próximo passo sugerido: " + documentSecurity.alerts().getFirst() + ".";
        }
        if (approval != null && approval.approvalRequired() && approval.checkpoints() != null && !approval.checkpoints().isEmpty()) {
            return "Próximo passo sugerido: " + approval.checkpoints().getFirst() + ".";
        }
        if (council == null || council.isEmpty()) {
            return "Próximo passo: aprofundar a consulta com contexto processual adicional.";
        }
        Object action = council.get(council.size() - 1).get("action");
        return "Próximo passo sugerido: " + String.valueOf(action) + ".";
    }
}
