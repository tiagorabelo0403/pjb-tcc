package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecoverySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRehabilitationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecurrenceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilitySuppressionSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalToolScopePolicy {

    public LegalAiConversationToolScopeSnapshot evaluate(LegalAiConversationRequest request,
                                                         String capability,
                                                         String version,
                                                         List<LegalAiToolDescriptor> tools,
                                                         LegalAiConversationDocumentSecuritySnapshot documentSecurity) {
        List<LegalAiToolDescriptor> effectiveTools = tools == null ? List.of() : List.copyOf(tools);
        String userProfile = normalize(request == null ? null : request.userProfile());
        String sigilo = normalize(contextValue(request, "sigilo"));
        String message = normalize(request == null ? null : request.message());
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>();
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>();
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>();
        List<String> reasons = new ArrayList<>();
        boolean restrictedDocumentState = documentSecurity != null && !Objects.equals(documentSecurity.status(), "CLEARED");
        for (LegalAiToolDescriptor tool : effectiveTools) {
            if (tool == null || tool.id() == null) {
                continue;
            }
            boolean citizenReadOnly = "cidadao".equals(userProfile) && !tool.readOnly();
            boolean sigiloSensitive = sigilo != null && (sigilo.contains("sigil") || sigilo.contains("restrit"))
                    && (tool.requiresStepUp() || !tool.readOnly() || tool.sourceLane().startsWith("MCP_"));
            boolean documentRestricted = restrictedDocumentState && !tool.readOnly();
            boolean workflowMutation = requestsSensitiveWrite(message) && !tool.readOnly();
            if (citizenReadOnly || sigiloSensitive || documentRestricted) {
                blockedToolIds.add(tool.id());
            } else {
                allowedToolIds.add(tool.id());
            }
            if ((tool.requiresStepUp() || workflowMutation) && !blockedToolIds.contains(tool.id())) {
                stepUpToolIds.add(tool.id());
            }
        }
        if (restrictedDocumentState) {
            reasons.add("Documentos em quarentena reduzem a malha para trilha read-only até saneamento material.");
        }
        if ("cidadao".equals(userProfile)) {
            reasons.add("Perfil cidadão opera apenas ferramentas read-only na conversa jurídica.");
        }
        if (sigilo != null && (sigilo.contains("sigil") || sigilo.contains("restrit"))) {
            reasons.add("Sigilo reforça escopo mínimo de ferramentas e step-up para atos sensíveis.");
        }
        if (requestsSensitiveWrite(message)) {
            reasons.add("Pedido com indício de minuta, protocolo ou ação mutável exige step-up nas ferramentas elegíveis.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("toolCount", effectiveTools.size());
        diagnostics.put("userProfile", userProfile);
        diagnostics.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        diagnostics.put("restrictedDocumentState", restrictedDocumentState);
        Map<String, Object> mcpPlan = nestedMap(request, "juridicaMcpPlan");
        diagnostics.put("mcpSelectionMode", mcpPlan.getOrDefault("selectionMode", restrictedDocumentState || (sigilo != null && sigilo.contains("sigil")) ? "PINNED_STRICT_TRUST_CHAIN" : "DISCOVERY_THEN_PIN"));
        diagnostics.put("mcpPinnedServers", mcpPlan.containsKey("pinnedServers")
                ? mcpPlan.get("pinnedServers")
                : effectiveTools.stream().filter(tool -> tool.mcpEnabled()).map(LegalAiToolDescriptor::sourceLane).distinct().toList());
        Map<String, Object> evaluation = nestedMap(mcpPlan, "evaluation");
        diagnostics.put("mcpQualityScore", evaluation.get("qualityScore"));
        diagnostics.put("mcpBenchmarkPassed", evaluation.get("passed"));
        diagnostics.put("mcpPromotionCandidates", evaluation.getOrDefault("promotionCandidates", List.of()));
        diagnostics.put("mcpDemotionCandidates", evaluation.getOrDefault("demotionCandidates", List.of()));
        String status = blockedToolIds.isEmpty()
                ? stepUpToolIds.isEmpty() ? "OPEN" : "STEP_UP_GATED"
                : allowedToolIds.isEmpty() ? "READONLY_LOCKED" : "PARTIAL";
        return new LegalAiConversationToolScopeSnapshot(
                status,
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    public LegalAiConversationToolScopeSnapshot enrichWithMcpPlan(LegalAiConversationToolScopeSnapshot snapshot,
                                                                  Map<String, Object> mcpPlan) {
        if (snapshot == null) {
            return null;
        }
        Map<String, Object> plan = mcpPlan == null ? Map.of() : ImmutableViewSupport.map(mcpPlan);
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("mcpSelectionMode", plan.getOrDefault("selectionMode", diagnostics.get("mcpSelectionMode")));
        diagnostics.put("mcpPinnedServers", plan.getOrDefault("pinnedServers", diagnostics.getOrDefault("mcpPinnedServers", List.of())));
        Map<String, Object> evaluation = nestedMap(plan, "evaluation");
        diagnostics.put("mcpQualityScore", evaluation.getOrDefault("qualityScore", diagnostics.get("mcpQualityScore")));
        diagnostics.put("mcpBenchmarkPassed", evaluation.getOrDefault("passed", diagnostics.get("mcpBenchmarkPassed")));
        diagnostics.put("mcpPromotionCandidates", evaluation.getOrDefault("promotionCandidates", diagnostics.getOrDefault("mcpPromotionCandidates", List.of())));
        diagnostics.put("mcpDemotionCandidates", evaluation.getOrDefault("demotionCandidates", diagnostics.getOrDefault("mcpDemotionCandidates", List.of())));
        diagnostics.put("mcpSkillIds", listOfMapsIds(plan.get("pinnedSkills"), "skillId"));
        diagnostics.put("mcpToolExampleIds", listOfMapsIds(plan.get("pinnedToolExamples"), "exampleId"));
        Map<String, Object> deliberation = nestedMap(plan, "deliberation");
        diagnostics.put("mcpDeliberationMode", deliberation.get("mode"));
        diagnostics.put("mcpDeliberationRequired", deliberation.get("required"));
        Map<String, Object> contextCompaction = nestedMap(plan, "contextCompaction");
        diagnostics.put("mcpContextCompactionPolicy", contextCompaction.get("policy"));
        diagnostics.put("mcpContextCompactionStatus", contextCompaction.get("status"));
        Map<String, Object> transcript = nestedMap(plan, "transcript");
        diagnostics.put("mcpTranscriptId", transcript.get("transcriptId"));
        diagnostics.put("mcpTranscriptMode", transcript.get("captureMode"));
        diagnostics.put("mcpTranscriptReplayReady", transcript.get("replayReady"));
        Map<String, Object> doctor = nestedMap(plan, "doctor");
        diagnostics.put("mcpDoctorStatus", doctor.get("status"));
        diagnostics.put("mcpDoctorReady", doctor.get("ready"));
        diagnostics.put("mcpDoctorOperationalMode", doctor.get("operationalMode"));
        Map<String, Object> evidencePromotion = nestedMap(plan, "evidencePromotion");
        diagnostics.put("mcpEvidencePromotionStatus", evidencePromotion.get("status"));
        diagnostics.put("mcpPromotedToolExampleIds", evidencePromotion.getOrDefault("promotedToolExampleIds", List.of()));
        diagnostics.put("mcpEvidenceApprovalLane", evidencePromotion.get("approvalLane"));
        diagnostics.put("mcpEvidenceScore", evidencePromotion.get("evidenceScore"));
        return new LegalAiConversationToolScopeSnapshot(
                snapshot.status(),
                snapshot.allowedToolIds(),
                snapshot.blockedToolIds(),
                snapshot.stepUpToolIds(),
                snapshot.reasons(),
                ImmutableViewSupport.map(diagnostics)
        );
    }


    public LegalAiConversationToolScopeSnapshot enrichWithSessionDoctor(LegalAiConversationToolScopeSnapshot snapshot,
                                                                        LegalAiConversationSessionDoctorSnapshot sessionDoctor) {
        if (snapshot == null || sessionDoctor == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("sessionDoctorStatus", sessionDoctor.status());
        diagnostics.put("sessionDoctorBlockedSurface", sessionDoctor.blockedSurface());
        diagnostics.put("sessionDoctorDriftDetected", sessionDoctor.driftDetected());
        diagnostics.put("sessionDoctorOperationalMode", sessionDoctor.operationalMode());
        diagnostics.put("sessionDoctorBlockedSkillIds", sessionDoctor.blockedSkillIds() == null ? List.of() : sessionDoctor.blockedSkillIds());
        diagnostics.put("sessionDoctorBlockedToolExampleIds", sessionDoctor.blockedToolExampleIds() == null ? List.of() : sessionDoctor.blockedToolExampleIds());
        diagnostics.put("sessionDoctorReasons", sessionDoctor.reasons() == null ? List.of() : sessionDoctor.reasons());
        if (sessionDoctor.blockedSurface()) {
            blockedToolIds.addAll(allowedToolIds);
            blockedToolIds.addAll(stepUpToolIds);
            allowedToolIds.clear();
            reasons.add("Doctor contínuo de sessão bloqueou a surface até replay vencedor e estabilização material.");
            return new LegalAiConversationToolScopeSnapshot(
                    "SESSION_BLOCKED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if (sessionDoctor.driftDetected()) {
            reasons.add("Doctor contínuo de sessão congelou reutilização automática de skills/examples neste turno.");
        }
        String status = sessionDoctor.driftDetected() && "OPEN".equalsIgnoreCase(snapshot.status())
                ? "SESSION_MONITORED"
                : snapshot.status();
        return new LegalAiConversationToolScopeSnapshot(
                status,
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }


    public LegalAiConversationToolScopeSnapshot enrichWithSessionBootstrap(LegalAiConversationToolScopeSnapshot snapshot,
                                                                           LegalAiConversationSessionBootstrapSnapshot sessionBootstrap) {
        if (snapshot == null || sessionBootstrap == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("sessionBootstrapStatus", sessionBootstrap.status());
        diagnostics.put("sessionBootstrapBlockedCapability", sessionBootstrap.blockedCapability());
        diagnostics.put("sessionBootstrapRepeatedDriftDetected", sessionBootstrap.repeatedDriftDetected());
        diagnostics.put("sessionBootstrapOperationalMode", sessionBootstrap.operationalMode());
        diagnostics.put("sessionBootstrapProfileGate", sessionBootstrap.profileGate());
        diagnostics.put("sessionBootstrapSigiloFence", sessionBootstrap.sigiloFence());
        diagnostics.put("sessionBootstrapMandatorySkillIds", sessionBootstrap.mandatorySkillIds() == null ? List.of() : sessionBootstrap.mandatorySkillIds());
        diagnostics.put("sessionBootstrapMandatoryToolExampleIds", sessionBootstrap.mandatoryToolExampleIds() == null ? List.of() : sessionBootstrap.mandatoryToolExampleIds());
        diagnostics.put("sessionBootstrapMissingSkillIds", sessionBootstrap.missingSkillIds() == null ? List.of() : sessionBootstrap.missingSkillIds());
        diagnostics.put("sessionBootstrapMissingToolExampleIds", sessionBootstrap.missingToolExampleIds() == null ? List.of() : sessionBootstrap.missingToolExampleIds());
        diagnostics.put("sessionBootstrapReasons", sessionBootstrap.reasons() == null ? List.of() : sessionBootstrap.reasons());
        if (sessionBootstrap.blockedCapability()) {
            LinkedHashSet<String> recoveryCandidateToolIds = new LinkedHashSet<>(allowedToolIds);
            recoveryCandidateToolIds.addAll(stepUpToolIds);
            diagnostics.put("sessionBootstrapRecoveryCandidateToolIds", List.copyOf(recoveryCandidateToolIds));
            blockedToolIds.addAll(allowedToolIds);
            blockedToolIds.addAll(stepUpToolIds);
            allowedToolIds.clear();
            reasons.add("Bootstrap contínuo de sessão bloqueou a capability até convergência do perfil, sigilo e replay operacional.");
            return new LegalAiConversationToolScopeSnapshot(
                    "SESSION_BOOTSTRAP_BLOCKED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if (sessionBootstrap.repeatedDriftDetected()) {
            stepUpToolIds.addAll(allowedToolIds);
            reasons.add("Bootstrap contínuo de sessão elevou a capability para gate assistido por drift repetido.");
        }
        if ((sessionBootstrap.missingSkillIds() != null && !sessionBootstrap.missingSkillIds().isEmpty())
                || (sessionBootstrap.missingToolExampleIds() != null && !sessionBootstrap.missingToolExampleIds().isEmpty())) {
            reasons.add("Bootstrap contínuo de sessão identificou cobertura mínima ausente de skills/examples para esta capability.");
        }
        String status = sessionBootstrap.repeatedDriftDetected() && "OPEN".equalsIgnoreCase(snapshot.status())
                ? "SESSION_BOOTSTRAP_GATED"
                : snapshot.status();
        return new LegalAiConversationToolScopeSnapshot(
                status,
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }



    public LegalAiConversationToolScopeSnapshot enrichWithCapabilityRecovery(LegalAiConversationToolScopeSnapshot snapshot,
                                                                             LegalAiConversationCapabilityRecoverySnapshot capabilityRecovery) {
        if (snapshot == null || capabilityRecovery == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("capabilityRecoveryStatus", capabilityRecovery.status());
        diagnostics.put("capabilityRecoveryEligible", capabilityRecovery.recoveryEligible());
        diagnostics.put("capabilityRecoveryRecovered", capabilityRecovery.capabilityRecovered());
        diagnostics.put("capabilityRecoveryLane", capabilityRecovery.recoveryLane());
        diagnostics.put("capabilityRecoveryCandidateToolIds", capabilityRecovery.recoveryCandidateToolIds() == null ? List.of() : capabilityRecovery.recoveryCandidateToolIds());
        diagnostics.put("capabilityRecoveryUnmetRequirements", capabilityRecovery.unmetRequirements() == null ? List.of() : capabilityRecovery.unmetRequirements());
        diagnostics.put("capabilityRecoveryReasons", capabilityRecovery.reasons() == null ? List.of() : capabilityRecovery.reasons());
        if (capabilityRecovery.capabilityRecovered()) {
            LinkedHashSet<String> recoveryCandidateToolIds = new LinkedHashSet<>(capabilityRecovery.recoveryCandidateToolIds() == null ? List.of() : capabilityRecovery.recoveryCandidateToolIds());
            if (recoveryCandidateToolIds.isEmpty()) {
                recoveryCandidateToolIds.addAll(stepUpToolIds);
            }
            allowedToolIds.addAll(recoveryCandidateToolIds);
            blockedToolIds.removeAll(recoveryCandidateToolIds);
            stepUpToolIds.addAll(recoveryCandidateToolIds);
            reasons.add("Capability recovery lane reabriu a capability em modo monitorado com gate assistido por evidência.");
            return new LegalAiConversationToolScopeSnapshot(
                    "SESSION_RECOVERY_GATED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if (capabilityRecovery.recoveryEligible()) {
            reasons.add("Capability recovery lane aguardando replay vencedor, doctor estável e cobertura mínima recomposta.");
        }
        return new LegalAiConversationToolScopeSnapshot(
                snapshot.status(),
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    public LegalAiConversationToolScopeSnapshot enrichWithCapabilityCooldown(LegalAiConversationToolScopeSnapshot snapshot,
                                                                             LegalAiConversationCapabilityCooldownSnapshot capabilityCooldown) {
        if (snapshot == null || capabilityCooldown == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("capabilityCooldownStatus", capabilityCooldown.status());
        diagnostics.put("capabilityCooldownLockActive", capabilityCooldown.lockActive());
        diagnostics.put("capabilityCooldownLockScope", capabilityCooldown.lockScope());
        diagnostics.put("capabilityCooldownLockKey", capabilityCooldown.lockKey());
        diagnostics.put("capabilityCooldownTurnsRemaining", capabilityCooldown.cooldownTurnsRemaining());
        diagnostics.put("capabilityCooldownBlockedCapability", capabilityCooldown.blockedCapability());
        diagnostics.put("capabilityCooldownBlockedToolIds", capabilityCooldown.blockedToolIds() == null ? List.of() : capabilityCooldown.blockedToolIds());
        diagnostics.put("capabilityCooldownReasons", capabilityCooldown.reasons() == null ? List.of() : capabilityCooldown.reasons());
        if (capabilityCooldown.lockActive()) {
            LinkedHashSet<String> cooldownBlockedToolIds = new LinkedHashSet<>(capabilityCooldown.blockedToolIds() == null ? List.of() : capabilityCooldown.blockedToolIds());
            if (cooldownBlockedToolIds.isEmpty()) {
                cooldownBlockedToolIds.addAll(stepUpToolIds);
            }
            allowedToolIds.removeAll(cooldownBlockedToolIds);
            stepUpToolIds.removeAll(cooldownBlockedToolIds);
            blockedToolIds.addAll(cooldownBlockedToolIds);
            reasons.add("Capability cooldown lock congelou os recovery candidates até estabilização da sessão/processo.");
            return new LegalAiConversationToolScopeSnapshot(
                    "CAPABILITY_COOLDOWN_LOCKED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("MONITORED".equalsIgnoreCase(capabilityCooldown.status())) {
            stepUpToolIds.addAll(allowedToolIds);
            reasons.add("Capability cooldown colocou a trilha em monitoramento assistido para evitar abre-fecha instável.");
            return new LegalAiConversationToolScopeSnapshot(
                    "CAPABILITY_COOLDOWN_MONITORED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        return new LegalAiConversationToolScopeSnapshot(
                snapshot.status(),
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    public LegalAiConversationToolScopeSnapshot enrichWithCapabilityRehabilitation(LegalAiConversationToolScopeSnapshot snapshot,
                                                                                   LegalAiConversationCapabilityRehabilitationSnapshot capabilityRehabilitation) {
        if (snapshot == null || capabilityRehabilitation == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("capabilityRehabilitationStatus", capabilityRehabilitation.status());
        diagnostics.put("capabilityRehabilitationRequired", capabilityRehabilitation.rehabilitationRequired());
        diagnostics.put("capabilityRehabilitationReleaseEligible", capabilityRehabilitation.releaseEligible());
        diagnostics.put("capabilityRehabilitationReleased", capabilityRehabilitation.capabilityReleased());
        diagnostics.put("capabilityRehabilitationReleaseLane", capabilityRehabilitation.releaseLane());
        diagnostics.put("capabilityRehabilitationStableWinningTurns", capabilityRehabilitation.stableWinningTurns());
        diagnostics.put("capabilityRehabilitationRequiredStableTurns", capabilityRehabilitation.requiredStableTurns());
        diagnostics.put("capabilityRehabilitationWindowTurnsRemaining", capabilityRehabilitation.rehabilitationWindowTurnsRemaining());
        diagnostics.put("capabilityRehabilitationReleasedToolIds", capabilityRehabilitation.releasedToolIds() == null ? List.of() : capabilityRehabilitation.releasedToolIds());
        diagnostics.put("capabilityRehabilitationBlockedToolIds", capabilityRehabilitation.blockedToolIds() == null ? List.of() : capabilityRehabilitation.blockedToolIds());
        diagnostics.put("capabilityRehabilitationUnmetRequirements", capabilityRehabilitation.unmetRequirements() == null ? List.of() : capabilityRehabilitation.unmetRequirements());
        diagnostics.put("capabilityRehabilitationReasons", capabilityRehabilitation.reasons() == null ? List.of() : capabilityRehabilitation.reasons());
        if (capabilityRehabilitation.capabilityReleased()) {
            LinkedHashSet<String> releasedToolIds = new LinkedHashSet<>(capabilityRehabilitation.releasedToolIds() == null ? List.of() : capabilityRehabilitation.releasedToolIds());
            allowedToolIds.addAll(releasedToolIds);
            blockedToolIds.removeAll(releasedToolIds);
            if ("REHABILITATION_STEP_UP_GATED".equalsIgnoreCase(capabilityRehabilitation.releaseLane())) {
                stepUpToolIds.addAll(releasedToolIds);
            }
            reasons.add("Janela formal de reabilitação liberou a capability após estabilidade vencedora suficiente por turno.");
            return new LegalAiConversationToolScopeSnapshot(
                    "CAPABILITY_REHABILITATED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        LinkedHashSet<String> rehabilitationBlockedToolIds = new LinkedHashSet<>(capabilityRehabilitation.blockedToolIds() == null ? List.of() : capabilityRehabilitation.blockedToolIds());
        if (!rehabilitationBlockedToolIds.isEmpty()) {
            blockedToolIds.addAll(rehabilitationBlockedToolIds);
            allowedToolIds.removeAll(rehabilitationBlockedToolIds);
        }
        reasons.add("Janela formal de reabilitação manteve a capability em contagem de estabilidade antes de liberar o reuse automático.");
        String status = "BLOCKED".equalsIgnoreCase(capabilityRehabilitation.status())
                ? "CAPABILITY_REHABILITATION_BLOCKED"
                : "CAPABILITY_REHABILITATION_PENDING";
        return new LegalAiConversationToolScopeSnapshot(
                status,
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private boolean requestsSensitiveWrite(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("minuta")
                || message.contains("protocol")
                || message.contains("peticion")
                || message.contains("assinar")
                || message.contains("enviar")
                || message.contains("submeter");
    }

    private String contextValue(LegalAiConversationRequest request, String key) {
        if (request == null || request.context() == null || key == null) {
            return null;
        }
        Object value = request.context().get(key);
        return value == null ? null : String.valueOf(value);
    }

    public LegalAiConversationToolScopeSnapshot enrichWithCapabilityRecurrence(LegalAiConversationToolScopeSnapshot snapshot,
                                                                               LegalAiConversationCapabilityRecurrenceSnapshot capabilityRecurrence) {
        if (snapshot == null || capabilityRecurrence == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("capabilityRecurrenceStatus", capabilityRecurrence.status());
        diagnostics.put("capabilityRecurrenceDetected", capabilityRecurrence.recurrenceDetected());
        diagnostics.put("capabilityRecurrenceProcessScoped", capabilityRecurrence.processScoped());
        diagnostics.put("capabilityRecurrenceRegistryKey", capabilityRecurrence.registryKey());
        diagnostics.put("capabilityRecurrenceCount", capabilityRecurrence.recurrenceCount());
        diagnostics.put("capabilityRecurrenceFailedRehabilitationCount", capabilityRecurrence.failedRehabilitationCount());
        diagnostics.put("capabilityRecurrenceRepeatedDriftDetected", capabilityRecurrence.repeatedDriftDetected());
        diagnostics.put("capabilityRecurrenceQuarantineHitCount", capabilityRecurrence.quarantineHitCount());
        diagnostics.put("capabilityRecurrenceRiskTier", capabilityRecurrence.riskTier());
        diagnostics.put("capabilityRecurrenceEscalationMode", capabilityRecurrence.escalationMode());
        diagnostics.put("capabilityRecurrenceBlockedToolIds", capabilityRecurrence.blockedToolIds() == null ? List.of() : capabilityRecurrence.blockedToolIds());
        diagnostics.put("capabilityRecurrenceUnmetRequirements", capabilityRecurrence.unmetRequirements() == null ? List.of() : capabilityRecurrence.unmetRequirements());
        diagnostics.put("capabilityRecurrenceReasons", capabilityRecurrence.reasons() == null ? List.of() : capabilityRecurrence.reasons());
        LinkedHashSet<String> recurrenceBlockedToolIds = new LinkedHashSet<>(capabilityRecurrence.blockedToolIds() == null ? List.of() : capabilityRecurrence.blockedToolIds());
        if (capabilityRecurrence.recurrenceDetected()) {
            reasons.add("Registry de reincidência por capability/processo endureceu a governança desta trilha antes de nova liberação automática.");
        }
        if ("LOCKED".equalsIgnoreCase(capabilityRecurrence.status())) {
            if (recurrenceBlockedToolIds.isEmpty()) {
                recurrenceBlockedToolIds.addAll(stepUpToolIds);
                recurrenceBlockedToolIds.addAll(allowedToolIds);
            }
            allowedToolIds.removeAll(recurrenceBlockedToolIds);
            stepUpToolIds.removeAll(recurrenceBlockedToolIds);
            blockedToolIds.addAll(recurrenceBlockedToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "PROCESS_RECURRENCE_LOCKED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("ESCALATED".equalsIgnoreCase(capabilityRecurrence.status())) {
            if (recurrenceBlockedToolIds.isEmpty()) {
                recurrenceBlockedToolIds.addAll(allowedToolIds);
            }
            stepUpToolIds.addAll(recurrenceBlockedToolIds);
            String status = "READONLY_LOCKED".equalsIgnoreCase(snapshot.status()) || "PROCESS_RECURRENCE_LOCKED".equalsIgnoreCase(snapshot.status())
                    ? snapshot.status()
                    : "PROCESS_RECURRENCE_ESCALATED";
            return new LegalAiConversationToolScopeSnapshot(
                    status,
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        return new LegalAiConversationToolScopeSnapshot(
                snapshot.status(),
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }


    public LegalAiConversationToolScopeSnapshot enrichWithCapabilitySuppression(LegalAiConversationToolScopeSnapshot snapshot,
                                                                                LegalAiConversationCapabilitySuppressionSnapshot capabilitySuppression) {
        if (snapshot == null || capabilitySuppression == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("capabilitySuppressionStatus", capabilitySuppression.status());
        diagnostics.put("capabilitySuppressionDetected", capabilitySuppression.suppressionDetected());
        diagnostics.put("capabilitySuppressionScope", capabilitySuppression.suppressionScope());
        diagnostics.put("capabilitySuppressionProcessClass", capabilitySuppression.processClass());
        diagnostics.put("capabilitySuppressionSigiloLevel", capabilitySuppression.sigiloLevel());
        diagnostics.put("capabilitySuppressionPolicyTier", capabilitySuppression.policyTier());
        diagnostics.put("capabilitySuppressionMode", capabilitySuppression.suppressionMode());
        diagnostics.put("capabilitySuppressionBlockedToolIds", capabilitySuppression.blockedToolIds() == null ? List.of() : capabilitySuppression.blockedToolIds());
        diagnostics.put("capabilitySuppressionStepUpToolIds", capabilitySuppression.elevatedStepUpToolIds() == null ? List.of() : capabilitySuppression.elevatedStepUpToolIds());
        diagnostics.put("capabilitySuppressionUnmetRequirements", capabilitySuppression.unmetRequirements() == null ? List.of() : capabilitySuppression.unmetRequirements());
        diagnostics.put("capabilitySuppressionReasons", capabilitySuppression.reasons() == null ? List.of() : capabilitySuppression.reasons());
        if (capabilitySuppression.suppressionDetected()) {
            reasons.add("Supressão adaptativa por classe processual e sigilo endureceu a capability antes de novo reuse automático.");
        }
        LinkedHashSet<String> suppressionBlockedToolIds = new LinkedHashSet<>(capabilitySuppression.blockedToolIds() == null ? List.of() : capabilitySuppression.blockedToolIds());
        LinkedHashSet<String> suppressionStepUpToolIds = new LinkedHashSet<>(capabilitySuppression.elevatedStepUpToolIds() == null ? List.of() : capabilitySuppression.elevatedStepUpToolIds());
        if ("LOCKED".equalsIgnoreCase(capabilitySuppression.status())) {
            if (suppressionBlockedToolIds.isEmpty()) {
                suppressionBlockedToolIds.addAll(allowedToolIds);
                suppressionBlockedToolIds.addAll(stepUpToolIds);
            }
            allowedToolIds.removeAll(suppressionBlockedToolIds);
            stepUpToolIds.removeAll(suppressionBlockedToolIds);
            blockedToolIds.addAll(suppressionBlockedToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "PROCESS_CLASS_SUPPRESSION_LOCKED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("ESCALATED".equalsIgnoreCase(capabilitySuppression.status())) {
            stepUpToolIds.addAll(suppressionStepUpToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "PROCESS_CLASS_SUPPRESSION_GATED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("MONITORED".equalsIgnoreCase(capabilitySuppression.status())) {
            return new LegalAiConversationToolScopeSnapshot(
                    "PROCESS_CLASS_SUPPRESSION_MONITORED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        return new LegalAiConversationToolScopeSnapshot(
                snapshot.status(),
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }



    public LegalAiConversationToolScopeSnapshot enrichWithTrustZone(LegalAiConversationToolScopeSnapshot snapshot,
                                                                    LegalAiConversationTrustZoneSnapshot trustZone) {
        if (snapshot == null || trustZone == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("trustZoneStatus", trustZone.status());
        diagnostics.put("trustZone", trustZone.trustZone());
        diagnostics.put("trustZoneSovereignBoundaryRequired", trustZone.sovereignBoundaryRequired());
        diagnostics.put("trustZoneSourceZone", trustZone.sourceZone());
        diagnostics.put("trustZoneAttachmentZone", trustZone.attachmentZone());
        diagnostics.put("trustZoneCapabilityZone", trustZone.capabilityZone());
        diagnostics.put("trustZoneMode", trustZone.trustZoneMode());
        diagnostics.put("trustZoneBlockedToolIds", trustZone.blockedToolIds() == null ? List.of() : trustZone.blockedToolIds());
        diagnostics.put("trustZoneStepUpToolIds", trustZone.elevatedStepUpToolIds() == null ? List.of() : trustZone.elevatedStepUpToolIds());
        diagnostics.put("trustZoneUnmetRequirements", trustZone.unmetRequirements() == null ? List.of() : trustZone.unmetRequirements());
        diagnostics.put("trustZoneReasons", trustZone.reasons() == null ? List.of() : trustZone.reasons());
        if (!"NOT_REQUIRED".equalsIgnoreCase(trustZone.status())) {
            reasons.add("Trust zone soberana reforçou a separação entre capability, fonte e anexo antes de novo reuse automático.");
        }
        LinkedHashSet<String> trustZoneBlockedToolIds = new LinkedHashSet<>(trustZone.blockedToolIds() == null ? List.of() : trustZone.blockedToolIds());
        LinkedHashSet<String> trustZoneStepUpToolIds = new LinkedHashSet<>(trustZone.elevatedStepUpToolIds() == null ? List.of() : trustZone.elevatedStepUpToolIds());
        if ("LOCKED".equalsIgnoreCase(trustZone.status())) {
            if (trustZoneBlockedToolIds.isEmpty()) {
                trustZoneBlockedToolIds.addAll(allowedToolIds);
                trustZoneBlockedToolIds.addAll(stepUpToolIds);
            }
            allowedToolIds.removeAll(trustZoneBlockedToolIds);
            stepUpToolIds.removeAll(trustZoneBlockedToolIds);
            blockedToolIds.addAll(trustZoneBlockedToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "TRUST_ZONE_LOCKED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("ESCALATED".equalsIgnoreCase(trustZone.status())) {
            blockedToolIds.addAll(trustZoneBlockedToolIds);
            allowedToolIds.removeAll(trustZoneBlockedToolIds);
            stepUpToolIds.addAll(trustZoneStepUpToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "TRUST_ZONE_GATED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("ENFORCED".equalsIgnoreCase(trustZone.status())) {
            stepUpToolIds.addAll(trustZoneStepUpToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "TRUST_ZONE_ENFORCED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        return new LegalAiConversationToolScopeSnapshot(
                snapshot.status(),
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }
    public LegalAiConversationToolScopeSnapshot enrichWithEvidenceProvenance(LegalAiConversationToolScopeSnapshot snapshot,
                                                                             LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance) {
        if (snapshot == null || evidenceProvenance == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUpToolIds = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        diagnostics.put("evidenceProvenanceStatus", evidenceProvenance.status());
        diagnostics.put("evidenceProvenanceTier", evidenceProvenance.effectiveEvidenceTier());
        diagnostics.put("evidenceSourceTier", evidenceProvenance.sourceEvidenceTier());
        diagnostics.put("evidenceAttachmentTier", evidenceProvenance.attachmentEvidenceTier());
        diagnostics.put("evidenceProvenanceMode", evidenceProvenance.sovereignProvenanceMode());
        diagnostics.put("ragPromotionStatus", evidenceProvenance.ragPromotionStatus());
        diagnostics.put("groundingPromotionStatus", evidenceProvenance.groundingPromotionStatus());
        diagnostics.put("draftPromotionStatus", evidenceProvenance.draftPromotionStatus());
        diagnostics.put("suggestionPromotionStatus", evidenceProvenance.suggestionPromotionStatus());
        diagnostics.put("capabilityRecoveryPromotionStatus", evidenceProvenance.capabilityRecoveryPromotionStatus());
        diagnostics.put("officialEvidenceIds", evidenceProvenance.officialEvidenceIds() == null ? List.of() : evidenceProvenance.officialEvidenceIds());
        diagnostics.put("institutionalControlledEvidenceIds", evidenceProvenance.institutionalControlledEvidenceIds() == null ? List.of() : evidenceProvenance.institutionalControlledEvidenceIds());
        diagnostics.put("derivedEvidenceIds", evidenceProvenance.derivedEvidenceIds() == null ? List.of() : evidenceProvenance.derivedEvidenceIds());
        diagnostics.put("untrustedEvidenceIds", evidenceProvenance.untrustedEvidenceIds() == null ? List.of() : evidenceProvenance.untrustedEvidenceIds());
        diagnostics.put("evidenceDescriptors", evidenceProvenance.evidenceDescriptors() == null ? List.of() : evidenceProvenance.evidenceDescriptors().stream().map(com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceDescriptor::asMap).toList());
        diagnostics.put("promotedRagEvidenceIds", evidenceProvenance.promotedRagEvidenceIds() == null ? List.of() : evidenceProvenance.promotedRagEvidenceIds());
        diagnostics.put("promotedGroundingEvidenceIds", evidenceProvenance.promotedGroundingEvidenceIds() == null ? List.of() : evidenceProvenance.promotedGroundingEvidenceIds());
        diagnostics.put("promotedDraftEvidenceIds", evidenceProvenance.promotedDraftEvidenceIds() == null ? List.of() : evidenceProvenance.promotedDraftEvidenceIds());
        diagnostics.put("promotedSuggestionEvidenceIds", evidenceProvenance.promotedSuggestionEvidenceIds() == null ? List.of() : evidenceProvenance.promotedSuggestionEvidenceIds());
        diagnostics.put("promotedCapabilityRecoveryEvidenceIds", evidenceProvenance.promotedCapabilityRecoveryEvidenceIds() == null ? List.of() : evidenceProvenance.promotedCapabilityRecoveryEvidenceIds());
        diagnostics.put("evidenceProvenanceBlockedToolIds", evidenceProvenance.blockedToolIds() == null ? List.of() : evidenceProvenance.blockedToolIds());
        diagnostics.put("evidenceProvenanceStepUpToolIds", evidenceProvenance.elevatedStepUpToolIds() == null ? List.of() : evidenceProvenance.elevatedStepUpToolIds());
        diagnostics.put("evidenceProvenanceUnmetRequirements", evidenceProvenance.unmetRequirements() == null ? List.of() : evidenceProvenance.unmetRequirements());
        diagnostics.put("evidenceProvenanceReasons", evidenceProvenance.reasons() == null ? List.of() : evidenceProvenance.reasons());
        if (!"NOT_REQUIRED".equalsIgnoreCase(evidenceProvenance.status())) {
            reasons.add("Registry soberano de proveniência reforçou a promotion de evidência antes de grounding, RAG, minuta e recovery lane.");
        }
        LinkedHashSet<String> evidenceBlockedToolIds = new LinkedHashSet<>(evidenceProvenance.blockedToolIds() == null ? List.of() : evidenceProvenance.blockedToolIds());
        LinkedHashSet<String> evidenceStepUpToolIds = new LinkedHashSet<>(evidenceProvenance.elevatedStepUpToolIds() == null ? List.of() : evidenceProvenance.elevatedStepUpToolIds());
        if ("LOCKED".equalsIgnoreCase(evidenceProvenance.status())) {
            blockedToolIds.addAll(evidenceBlockedToolIds);
            allowedToolIds.removeAll(evidenceBlockedToolIds);
            stepUpToolIds.removeAll(evidenceBlockedToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "EVIDENCE_PROVENANCE_LOCKED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("ESCALATED".equalsIgnoreCase(evidenceProvenance.status())) {
            blockedToolIds.addAll(evidenceBlockedToolIds);
            allowedToolIds.removeAll(evidenceBlockedToolIds);
            stepUpToolIds.addAll(evidenceStepUpToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "EVIDENCE_PROVENANCE_GATED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        if ("ENFORCED".equalsIgnoreCase(evidenceProvenance.status())) {
            stepUpToolIds.addAll(evidenceStepUpToolIds);
            return new LegalAiConversationToolScopeSnapshot(
                    "EVIDENCE_PROVENANCE_ENFORCED",
                    List.copyOf(allowedToolIds),
                    List.copyOf(blockedToolIds),
                    List.copyOf(stepUpToolIds),
                    List.copyOf(reasons),
                    ImmutableViewSupport.map(diagnostics)
            );
        }
        return new LegalAiConversationToolScopeSnapshot(
                snapshot.status(),
                List.copyOf(allowedToolIds),
                List.copyOf(blockedToolIds),
                List.copyOf(stepUpToolIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }



    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(LegalAiConversationRequest request, String key) {
        if (request == null || request.context() == null || key == null) {
            return Map.of();
        }
        Object value = request.context().get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || key == null) {
            return Map.of();
        }
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }



    @SuppressWarnings("unchecked")
    private List<String> listOfMapsIds(Object value, String key) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(map -> map.get(key))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
