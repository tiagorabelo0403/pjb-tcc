package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer.LegalConversationSanitizationResult;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTraceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiApprovalDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalSensitiveActionApprovalService {

    public LegalAiConversationApprovalSnapshot evaluate(LegalAiConversationRequest request,
                                                        String capability,
                                                        String version,
                                                        LegalAiApprovalDescriptor descriptor,
                                                        LegalAiConversationTraceSnapshot traceSnapshot,
                                                        LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                        LegalAiConversationToolScopeSnapshot toolScope,
                                                        LegalConversationSanitizationResult sanitization,
                                                        int retainedTurnCount) {
        Map<String, Object> approvalPolicy = descriptor == null || descriptor.approvalPolicy() == null ? Map.of() : descriptor.approvalPolicy();
        boolean descriptorApproval = descriptor != null && descriptor.approvalRequired();
        boolean descriptorStepUp = descriptor != null && descriptor.stepUpRequired();
        boolean quarantineReview = documentSecurity != null && Objects.equals(documentSecurity.status(), "HUMAN_REVIEW_REQUIRED");
        boolean quarantined = documentSecurity != null && !Objects.equals(documentSecurity.status(), "CLEARED");
        boolean promptInjectionDetected = sanitization != null && sanitization.snapshot() != null && sanitization.snapshot().promptInjectionDetected();
        boolean stepUpTools = toolScope != null && toolScope.stepUpToolIds() != null && !toolScope.stepUpToolIds().isEmpty();
        boolean blockedByToolScope = toolScope != null && toolScope.blockedToolIds() != null && !toolScope.blockedToolIds().isEmpty();
        String evidenceApprovalLane = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("mcpEvidenceApprovalLane", ""));
        String sessionDoctorStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("sessionDoctorStatus", ""));
        boolean sessionDoctorBlocked = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionDoctorBlockedSurface"));
        boolean sessionDoctorDrift = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionDoctorDriftDetected"));
        String sessionBootstrapStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("sessionBootstrapStatus", ""));
        boolean sessionBootstrapBlocked = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionBootstrapBlockedCapability"));
        boolean sessionBootstrapDrift = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("sessionBootstrapRepeatedDriftDetected"));
        String sessionBootstrapSigiloFence = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("sessionBootstrapSigiloFence", ""));
        String capabilityRecoveryStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecoveryStatus", ""));
        boolean capabilityRecoveryRecovered = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityRecoveryRecovered"));
        String capabilityRecoveryLane = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecoveryLane", ""));
        String capabilityCooldownStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityCooldownStatus", ""));
        boolean capabilityCooldownLocked = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityCooldownLockActive"));
        String capabilityCooldownScope = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityCooldownLockScope", ""));
        String capabilityRehabilitationStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRehabilitationStatus", ""));
        boolean capabilityRehabilitationReleased = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityRehabilitationReleased"));
        String capabilityRehabilitationReleaseLane = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRehabilitationReleaseLane", ""));
        String capabilityRecurrenceStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecurrenceStatus", ""));
        boolean capabilityRecurrenceDetected = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilityRecurrenceDetected"));
        String capabilityRecurrenceRiskTier = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecurrenceRiskTier", ""));
        String capabilityRecurrenceEscalationMode = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecurrenceEscalationMode", ""));
        String capabilitySuppressionStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilitySuppressionStatus", ""));
        boolean capabilitySuppressionDetected = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("capabilitySuppressionDetected"));
        String capabilitySuppressionMode = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilitySuppressionMode", ""));
        String capabilitySuppressionPolicyTier = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilitySuppressionPolicyTier", ""));
        String trustZoneStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZoneStatus", ""));
        String trustZone = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZone", ""));
        boolean trustZoneBoundaryRequired = toolScope != null && toolScope.diagnostics() != null && Boolean.TRUE.equals(toolScope.diagnostics().get("trustZoneSovereignBoundaryRequired"));
        String trustZoneMode = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZoneMode", ""));
        String trustZoneSourceZone = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZoneSourceZone", ""));
        String trustZoneAttachmentZone = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZoneAttachmentZone", ""));
        String trustZoneCapabilityZone = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("trustZoneCapabilityZone", ""));
        String evidenceProvenanceStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("evidenceProvenanceStatus", ""));
        String evidenceProvenanceTier = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("evidenceProvenanceTier", ""));
        String evidenceSourceTier = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("evidenceSourceTier", ""));
        String evidenceAttachmentTier = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("evidenceAttachmentTier", ""));
        String evidenceProvenanceMode = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("evidenceProvenanceMode", ""));
        String ragPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("ragPromotionStatus", ""));
        String groundingPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("groundingPromotionStatus", ""));
        String draftPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("draftPromotionStatus", ""));
        String suggestionPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("suggestionPromotionStatus", ""));
        String capabilityRecoveryPromotionStatus = toolScope == null || toolScope.diagnostics() == null ? null : String.valueOf(toolScope.diagnostics().getOrDefault("capabilityRecoveryPromotionStatus", ""));
        boolean evidenceReview = "STRICT_REVIEW".equalsIgnoreCase(evidenceApprovalLane) || "HUMAN_REVIEW_REQUIRED".equalsIgnoreCase(evidenceApprovalLane);
        boolean evidenceStepUp = "STEP_UP_REQUIRED".equalsIgnoreCase(evidenceApprovalLane);
        boolean sessionDoctorReview = sessionDoctorBlocked || "BLOCKED".equalsIgnoreCase(sessionDoctorStatus);
        boolean sessionDoctorStepUp = !sessionDoctorReview && (sessionDoctorDrift || "DEGRADED".equalsIgnoreCase(sessionDoctorStatus));
        boolean sessionBootstrapReview = sessionBootstrapBlocked || "BLOCKED".equalsIgnoreCase(sessionBootstrapStatus);
        boolean sessionBootstrapStepUp = !sessionBootstrapReview && (sessionBootstrapDrift || "DEGRADED".equalsIgnoreCase(sessionBootstrapStatus) || "DEGRADED".equalsIgnoreCase(sessionBootstrapSigiloFence));
        boolean capabilityRecoveryReview = "DENIED".equalsIgnoreCase(capabilityRecoveryStatus);
        boolean capabilityRecoveryStepUp = capabilityRecoveryRecovered || "RECOVERY_STEP_UP_MONITORED".equalsIgnoreCase(capabilityRecoveryLane);
        boolean capabilityCooldownReview = capabilityCooldownLocked;
        boolean capabilityCooldownStepUp = !capabilityCooldownReview && "MONITORED".equalsIgnoreCase(capabilityCooldownStatus);
        boolean capabilityRehabilitationReview = "BLOCKED".equalsIgnoreCase(capabilityRehabilitationStatus);
        boolean capabilityRehabilitationStepUp = capabilityRehabilitationReleased || "REHABILITATION_STEP_UP_GATED".equalsIgnoreCase(capabilityRehabilitationReleaseLane) || "MONITORED".equalsIgnoreCase(capabilityRehabilitationStatus);
        boolean capabilityRecurrenceReview = "LOCKED".equalsIgnoreCase(capabilityRecurrenceStatus)
                || "PROCESS_SCOPED_HARD_LOCK".equalsIgnoreCase(capabilityRecurrenceEscalationMode)
                || "SESSION_HARD_LOCK".equalsIgnoreCase(capabilityRecurrenceEscalationMode)
                || "PROCESS_SCOPED_HUMAN_REVIEW".equalsIgnoreCase(capabilityRecurrenceEscalationMode);
        boolean capabilityRecurrenceStepUp = !capabilityRecurrenceReview
                && ("ESCALATED".equalsIgnoreCase(capabilityRecurrenceStatus)
                || "PROCESS_SCOPED_STEP_UP".equalsIgnoreCase(capabilityRecurrenceEscalationMode));
        boolean capabilitySuppressionReview = "LOCKED".equalsIgnoreCase(capabilitySuppressionStatus)
                || "CLASS_SIGILO_HARD_LOCK".equalsIgnoreCase(capabilitySuppressionMode)
                || "CLASS_SIGILO_HUMAN_REVIEW".equalsIgnoreCase(capabilitySuppressionMode);
        boolean capabilitySuppressionStepUp = !capabilitySuppressionReview
                && ("ESCALATED".equalsIgnoreCase(capabilitySuppressionStatus)
                || "MONITORED".equalsIgnoreCase(capabilitySuppressionStatus)
                || "CLASS_STEP_UP_GATED".equalsIgnoreCase(capabilitySuppressionMode));
        boolean trustZoneReview = "LOCKED".equalsIgnoreCase(trustZoneStatus)
                || "CRITICAL".equalsIgnoreCase(trustZone)
                || "SOVEREIGN_HARD_LOCK".equalsIgnoreCase(trustZoneMode);
        boolean trustZoneStepUp = !trustZoneReview
                && ("ESCALATED".equalsIgnoreCase(trustZoneStatus)
                || "ENFORCED".equalsIgnoreCase(trustZoneStatus)
                || "SIGILOSA".equalsIgnoreCase(trustZone)
                || (trustZoneBoundaryRequired && "MUTATING".equalsIgnoreCase(trustZoneCapabilityZone)));
        boolean evidenceProvenanceReview = "LOCKED".equalsIgnoreCase(evidenceProvenanceStatus)
                || "BLOCKED".equalsIgnoreCase(ragPromotionStatus)
                || "BLOCKED".equalsIgnoreCase(groundingPromotionStatus)
                || "BLOCKED".equalsIgnoreCase(draftPromotionStatus)
                || "BLOCKED".equalsIgnoreCase(suggestionPromotionStatus)
                || "BLOCKED".equalsIgnoreCase(capabilityRecoveryPromotionStatus)
                || "UNTRUSTED_DOCUMENT".equalsIgnoreCase(evidenceProvenanceTier)
                || "SOVEREIGN_PROVENANCE_HARD_LOCK".equalsIgnoreCase(evidenceProvenanceMode);
        boolean evidenceProvenanceStepUp = !evidenceProvenanceReview
                && ("ESCALATED".equalsIgnoreCase(evidenceProvenanceStatus)
                || "ENFORCED".equalsIgnoreCase(evidenceProvenanceStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(ragPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(groundingPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(draftPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(suggestionPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(capabilityRecoveryPromotionStatus)
                || "DERIVED_DOCUMENT".equalsIgnoreCase(evidenceProvenanceTier)
                || "INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(evidenceProvenanceTier));
        boolean approvalRequired = descriptorApproval || quarantined || blockedByToolScope || evidenceReview || evidenceStepUp || sessionDoctorReview || sessionDoctorStepUp || sessionBootstrapReview || sessionBootstrapStepUp || capabilityRecoveryReview || capabilityRecoveryStepUp || capabilityCooldownReview || capabilityCooldownStepUp || capabilityRehabilitationReview || capabilityRehabilitationStepUp || capabilityRecurrenceReview || capabilityRecurrenceStepUp || capabilitySuppressionReview || capabilitySuppressionStepUp || trustZoneReview || trustZoneStepUp || evidenceProvenanceReview || evidenceProvenanceStepUp;
        boolean stepUpRequired = !quarantineReview && (descriptorStepUp || stepUpTools || evidenceStepUp || sessionDoctorStepUp || sessionBootstrapStepUp || capabilityRecoveryStepUp || capabilityCooldownStepUp || capabilityRehabilitationStepUp || capabilityRecurrenceStepUp || capabilitySuppressionStepUp || trustZoneStepUp || evidenceProvenanceStepUp);
        String status = !approvalRequired
                ? "AUTO_READONLY"
                : quarantineReview || promptInjectionDetected || "HUMAN_REVIEW_REQUIRED".equalsIgnoreCase(evidenceApprovalLane) || sessionDoctorReview || sessionBootstrapReview || capabilityRecoveryReview || capabilityCooldownReview || capabilityRehabilitationReview || capabilityRecurrenceReview || capabilitySuppressionReview || trustZoneReview || evidenceProvenanceReview ? "HUMAN_REVIEW_REQUIRED" : stepUpRequired ? "STEP_UP_REQUIRED" : "READONLY_RESTRICTED";
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (descriptor != null && descriptor.approvalReasons() != null) {
            reasons.addAll(descriptor.approvalReasons());
        }
        if (quarantined) {
            reasons.add("DOCUMENT_SECURITY_" + documentSecurity.status());
        }
        if (promptInjectionDetected) {
            reasons.add("PROMPT_INJECTION_REVIEW");
        }
        if (toolScope != null) {
            reasons.add("TOOL_SCOPE_" + toolScope.status());
        }
        if (evidenceApprovalLane != null && !evidenceApprovalLane.isBlank()) {
            reasons.add("MCP_EVIDENCE_LANE_" + evidenceApprovalLane);
        }
        if (sessionDoctorStatus != null && !sessionDoctorStatus.isBlank()) {
            reasons.add("SESSION_DOCTOR_" + sessionDoctorStatus);
        }
        if (sessionBootstrapStatus != null && !sessionBootstrapStatus.isBlank()) {
            reasons.add("SESSION_BOOTSTRAP_" + sessionBootstrapStatus);
        }
        if (capabilityRecoveryStatus != null && !capabilityRecoveryStatus.isBlank()) {
            reasons.add("CAPABILITY_RECOVERY_" + capabilityRecoveryStatus);
        }
        if (capabilityCooldownStatus != null && !capabilityCooldownStatus.isBlank()) {
            reasons.add("CAPABILITY_COOLDOWN_" + capabilityCooldownStatus);
        }
        if (capabilityRehabilitationStatus != null && !capabilityRehabilitationStatus.isBlank()) {
            reasons.add("CAPABILITY_REHABILITATION_" + capabilityRehabilitationStatus);
        }
        if (capabilityRecurrenceStatus != null && !capabilityRecurrenceStatus.isBlank()) {
            reasons.add("CAPABILITY_RECURRENCE_" + capabilityRecurrenceStatus);
        }
        if (capabilitySuppressionStatus != null && !capabilitySuppressionStatus.isBlank()) {
            reasons.add("CAPABILITY_SUPPRESSION_" + capabilitySuppressionStatus);
        }
        if (trustZoneStatus != null && !trustZoneStatus.isBlank()) {
            reasons.add("TRUST_ZONE_" + trustZoneStatus);
        }
        if (evidenceProvenanceStatus != null && !evidenceProvenanceStatus.isBlank()) {
            reasons.add("EVIDENCE_PROVENANCE_" + evidenceProvenanceStatus);
        }
        List<String> checkpoints = new ArrayList<>();
        if (quarantineReview) {
            checkpoints.add("Executar revisão humana antes de promover anexo, contexto externo ou ação sensível para a conversa jurídica.");
        } else if (stepUpRequired) {
            checkpoints.add("Exigir step-up credencial antes de liberar ferramenta sensível, escrita jurídica controlada ou fluxo mutável.");
        }
        checkpoints.add("Manter a conversa em trilha read-only até saneamento documental e aprovação material.");
        if (documentSecurity != null && documentSecurity.blockedSources() != null && !documentSecurity.blockedSources().isEmpty()) {
            checkpoints.add("Remover ou substituir fontes fora da allowlist oficial antes de expandir grounding ou RAG.");
        }
        if (toolScope != null && toolScope.blockedToolIds() != null && !toolScope.blockedToolIds().isEmpty()) {
            checkpoints.add("Ferramentas fora do escopo atual permanecem bloqueadas para este turno.");
        }
        if (evidenceReview) {
            checkpoints.add("A trilha de evidência do MCP exige revisão reforçada antes de promover examples vencedores ou liberar ação sensível.");
        } else if (evidenceStepUp) {
            checkpoints.add("A trilha de evidência do MCP exige step-up antes de reaproveitar examples promovidos neste turno.");
        }
        if (sessionDoctorReview) {
            checkpoints.add("O doctor contínuo de sessão bloqueou a surface até novo replay vencedor e estabilização do histórico retido.");
        } else if (sessionDoctorStepUp) {
            checkpoints.add("O doctor contínuo de sessão exige step-up antes de reaproveitar skills ou examples nesta sessão.");
        }
        if (sessionBootstrapReview) {
            checkpoints.add("O bootstrap contínuo de sessão bloqueou a capability até convergência entre perfil jurídico, sigilo e replay operacional.");
        } else if (sessionBootstrapStepUp) {
            checkpoints.add("O bootstrap contínuo de sessão exige gate assistido antes de manter reuse automático nesta capability.");
        }
        if (capabilityRecoveryReview) {
            checkpoints.add("A capability recovery lane negou reabertura da capability por bloqueio estrutural de perfil jurídico ou de sigilo.");
        } else if (capabilityRecoveryStepUp) {
            checkpoints.add("A capability recovery lane reabriu a capability em modo monitorado com step-up obrigatório e replay vencedor confirmado.");
        }
        if (capabilityCooldownReview) {
            checkpoints.add("O capability cooldown lock congelou a capability para evitar abre-fecha instável até estabilização do histórico e do processo.");
        } else if (capabilityCooldownStepUp) {
            checkpoints.add("O capability cooldown colocou a capability em monitoramento assistido antes de liberar reuse automático novamente.");
        }
        if (capabilityRehabilitationReview) {
            checkpoints.add("A janela formal de reabilitação negou liberação da capability porque o bloqueio estrutural de recuperação, perfil ou sigilo ainda persiste.");
        } else if (capabilityRehabilitationStepUp) {
            checkpoints.add("A janela formal de reabilitação exige gate assistido até acumular estabilidade vencedora suficiente por turno.");
        }
        if (capabilityRecurrenceReview) {
            checkpoints.add("O registry de reincidência por capability/processo bloqueou a trilha até nova convergência material, porque a mesma capability voltou a oscilar de forma repetida.");
        } else if (capabilityRecurrenceStepUp) {
            checkpoints.add("O registry de reincidência por capability/processo elevou a lane para gate assistido até cessar a reincidência operacional.");
        }
        if (capabilitySuppressionReview) {
            checkpoints.add("A supressão adaptativa por classe processual e sigilo bloqueou a capability até sair do ramo/sigilo crítico ou até nova convergência material segura.");
        } else if (capabilitySuppressionStepUp) {
            checkpoints.add("A supressão adaptativa por classe processual e sigilo elevou a lane para gate assistido antes de reaproveitar tools, skills ou examples sensíveis.");
        }
        if (trustZoneReview) {
            checkpoints.add("A trust zone soberana bloqueou a capability porque fonte, anexo ou sigilo empurraram o turno para fronteira crítica não reutilizável.");
        } else if (trustZoneStepUp) {
            checkpoints.add("A trust zone soberana exige gate assistido antes de misturar capability mutável com fonte oficial, anexo controlado ou sigilo reforçado.");
        }
        if (evidenceProvenanceReview) {
            checkpoints.add("A proveniência soberana bloqueou promotion para grounding, RAG, minuta, suggestion flow ou recovery lane porque a cadeia efetiva caiu em evidência não confiável ou derivada fora da cerca aceita.");
        } else if (evidenceProvenanceStepUp) {
            checkpoints.add("A proveniência soberana exige gate assistido antes de promover evidência institucional ou derivada para grounding, RAG, minuta ou recovery lane.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("traceId", traceSnapshot == null ? null : traceSnapshot.traceId());
        diagnostics.put("turnId", traceSnapshot == null ? null : traceSnapshot.turnId());
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("requestedAttachmentCount", request == null || request.attachments() == null ? 0 : request.attachments().size());
        diagnostics.put("retainedTurnCount", retainedTurnCount);
        diagnostics.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        diagnostics.put("toolScopeStatus", toolScope == null ? null : toolScope.status());
        diagnostics.put("blockedToolIds", toolScope == null || toolScope.blockedToolIds() == null ? List.of() : toolScope.blockedToolIds());
        diagnostics.put("allowedToolIds", toolScope == null || toolScope.allowedToolIds() == null ? List.of() : toolScope.allowedToolIds());
        diagnostics.put("stepUpToolIds", toolScope == null || toolScope.stepUpToolIds() == null ? List.of() : toolScope.stepUpToolIds());
        diagnostics.put("promptInjectionDetected", promptInjectionDetected);
        diagnostics.put("mcpEvidenceApprovalLane", evidenceApprovalLane);
        diagnostics.put("sessionDoctorStatus", sessionDoctorStatus);
        diagnostics.put("sessionDoctorBlockedSurface", sessionDoctorBlocked);
        diagnostics.put("sessionDoctorDriftDetected", sessionDoctorDrift);
        diagnostics.put("sessionBootstrapStatus", sessionBootstrapStatus);
        diagnostics.put("sessionBootstrapBlockedCapability", sessionBootstrapBlocked);
        diagnostics.put("sessionBootstrapRepeatedDriftDetected", sessionBootstrapDrift);
        diagnostics.put("sessionBootstrapSigiloFence", sessionBootstrapSigiloFence);
        diagnostics.put("capabilityRecoveryStatus", capabilityRecoveryStatus);
        diagnostics.put("capabilityRecoveryRecovered", capabilityRecoveryRecovered);
        diagnostics.put("capabilityRecoveryLane", capabilityRecoveryLane);
        diagnostics.put("capabilityCooldownStatus", capabilityCooldownStatus);
        diagnostics.put("capabilityCooldownLockActive", capabilityCooldownLocked);
        diagnostics.put("capabilityCooldownLockScope", capabilityCooldownScope);
        diagnostics.put("capabilityRehabilitationStatus", capabilityRehabilitationStatus);
        diagnostics.put("capabilityRehabilitationReleased", capabilityRehabilitationReleased);
        diagnostics.put("capabilityRehabilitationReleaseLane", capabilityRehabilitationReleaseLane);
        diagnostics.put("capabilityRecurrenceStatus", capabilityRecurrenceStatus);
        diagnostics.put("capabilityRecurrenceDetected", capabilityRecurrenceDetected);
        diagnostics.put("capabilityRecurrenceRiskTier", capabilityRecurrenceRiskTier);
        diagnostics.put("capabilityRecurrenceEscalationMode", capabilityRecurrenceEscalationMode);
        diagnostics.put("capabilitySuppressionStatus", capabilitySuppressionStatus);
        diagnostics.put("capabilitySuppressionDetected", capabilitySuppressionDetected);
        diagnostics.put("capabilitySuppressionMode", capabilitySuppressionMode);
        diagnostics.put("capabilitySuppressionPolicyTier", capabilitySuppressionPolicyTier);
        diagnostics.put("trustZoneStatus", trustZoneStatus);
        diagnostics.put("trustZone", trustZone);
        diagnostics.put("trustZoneSovereignBoundaryRequired", trustZoneBoundaryRequired);
        diagnostics.put("trustZoneMode", trustZoneMode);
        diagnostics.put("trustZoneSourceZone", trustZoneSourceZone);
        diagnostics.put("trustZoneAttachmentZone", trustZoneAttachmentZone);
        diagnostics.put("trustZoneCapabilityZone", trustZoneCapabilityZone);
        diagnostics.put("evidenceProvenanceStatus", evidenceProvenanceStatus);
        diagnostics.put("evidenceProvenanceTier", evidenceProvenanceTier);
        diagnostics.put("evidenceSourceTier", evidenceSourceTier);
        diagnostics.put("evidenceAttachmentTier", evidenceAttachmentTier);
        diagnostics.put("evidenceProvenanceMode", evidenceProvenanceMode);
        diagnostics.put("ragPromotionStatus", ragPromotionStatus);
        diagnostics.put("groundingPromotionStatus", groundingPromotionStatus);
        diagnostics.put("draftPromotionStatus", draftPromotionStatus);
        diagnostics.put("suggestionPromotionStatus", suggestionPromotionStatus);
        diagnostics.put("capabilityRecoveryPromotionStatus", capabilityRecoveryPromotionStatus);
        diagnostics.put("capabilityRecoveryCandidateToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecoveryCandidateToolIds", List.of()));
        diagnostics.put("capabilityRecoveryUnmetRequirements", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecoveryUnmetRequirements", List.of()));
        diagnostics.put("capabilityCooldownTurnsRemaining", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityCooldownTurnsRemaining", 0));
        diagnostics.put("capabilityCooldownBlockedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityCooldownBlockedToolIds", List.of()));
        diagnostics.put("capabilityRehabilitationStableWinningTurns", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRehabilitationStableWinningTurns", 0));
        diagnostics.put("capabilityRehabilitationRequiredStableTurns", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRehabilitationRequiredStableTurns", 0));
        diagnostics.put("capabilityRehabilitationWindowTurnsRemaining", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRehabilitationWindowTurnsRemaining", 0));
        diagnostics.put("capabilityRehabilitationReleasedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRehabilitationReleasedToolIds", List.of()));
        diagnostics.put("capabilityRehabilitationBlockedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRehabilitationBlockedToolIds", List.of()));
        diagnostics.put("capabilityRehabilitationUnmetRequirements", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRehabilitationUnmetRequirements", List.of()));
        diagnostics.put("capabilityRecurrenceRegistryKey", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("capabilityRecurrenceRegistryKey"));
        diagnostics.put("capabilityRecurrenceCount", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRecurrenceCount", 0));
        diagnostics.put("capabilityRecurrenceFailedRehabilitationCount", toolScope == null || toolScope.diagnostics() == null ? 0 : toolScope.diagnostics().getOrDefault("capabilityRecurrenceFailedRehabilitationCount", 0));
        diagnostics.put("capabilityRecurrenceBlockedToolIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecurrenceBlockedToolIds", List.of()));
        diagnostics.put("capabilityRecurrenceUnmetRequirements", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("capabilityRecurrenceUnmetRequirements", List.of()));
        diagnostics.put("sessionDoctorBlockedSkillIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("sessionDoctorBlockedSkillIds", List.of()));
        diagnostics.put("sessionDoctorBlockedToolExampleIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("sessionDoctorBlockedToolExampleIds", List.of()));
        diagnostics.put("mcpPromotedToolExampleIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("mcpPromotedToolExampleIds", List.of()));
        diagnostics.put("mcpEvidencePromotionStatus", toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpEvidencePromotionStatus"));
        diagnostics.put("officialEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("officialEvidenceIds", List.of()));
        diagnostics.put("institutionalControlledEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("institutionalControlledEvidenceIds", List.of()));
        diagnostics.put("derivedEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("derivedEvidenceIds", List.of()));
        diagnostics.put("untrustedEvidenceIds", toolScope == null || toolScope.diagnostics() == null ? List.of() : toolScope.diagnostics().getOrDefault("untrustedEvidenceIds", List.of()));
        diagnostics.put("sensitiveWriteBlockedByDefault", Boolean.TRUE.equals(approvalPolicy.get("sensitiveWriteBlockedByDefault")));
        diagnostics.put("mutatingToolsAllowed", Boolean.TRUE.equals(approvalPolicy.get("mutatingToolsAllowed")));
        return new LegalAiConversationApprovalSnapshot(
                status,
                approvalRequired,
                stepUpRequired,
                List.copyOf(reasons),
                List.copyOf(checkpoints),
                ImmutableViewSupport.map(diagnostics)
        );
    }
}
