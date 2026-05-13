package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilitySuppressionSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationTrustZoneService {

    public LegalAiConversationTrustZoneSnapshot inspect(LegalAiConversationRequest request,
                                                        String capability,
                                                        String version,
                                                        LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                        LegalAiConversationToolScopeSnapshot toolScope,
                                                        LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                        LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                        LegalAiConversationCapabilitySuppressionSnapshot capabilitySuppression) {
        boolean processScoped = request != null && request.processoId() != null && !request.processoId().isBlank();
        String userProfile = normalize(request == null ? null : request.userProfile());
        String sigilo = normalize(contextValue(request, "sigilo"));
        boolean sigiloSensitive = sigilo != null && (sigilo.contains("SIGIL") || sigilo.contains("RESTRIT") || sigilo.contains("SEGRED") || sigilo.contains("CONFID"));
        boolean doctorBlocked = sessionDoctor != null && (sessionDoctor.blockedSurface() || "BLOCKED".equalsIgnoreCase(sessionDoctor.status()));
        boolean bootstrapBlocked = sessionBootstrap != null && (sessionBootstrap.blockedCapability() || "BLOCKED".equalsIgnoreCase(sessionBootstrap.status()));
        boolean suppressionLocked = capabilitySuppression != null && "LOCKED".equalsIgnoreCase(capabilitySuppression.status());
        boolean suppressionEscalated = capabilitySuppression != null && ("ESCALATED".equalsIgnoreCase(capabilitySuppression.status()) || "MONITORED".equalsIgnoreCase(capabilitySuppression.status()));
        boolean blockedSources = documentSecurity != null && documentSecurity.blockedSources() != null && !documentSecurity.blockedSources().isEmpty();
        boolean quarantinedAttachments = documentSecurity != null && documentSecurity.quarantinedAttachments() != null && !documentSecurity.quarantinedAttachments().isEmpty();
        boolean allowedAttachments = documentSecurity != null && documentSecurity.allowedAttachments() != null && !documentSecurity.allowedAttachments().isEmpty();
        boolean allowlistedSources = documentSecurity != null && documentSecurity.allowlistedSources() != null && !documentSecurity.allowlistedSources().isEmpty();
        String sourceZone = resolveSourceZone(documentSecurity, allowlistedSources, blockedSources, userProfile);
        String attachmentZone = resolveAttachmentZone(documentSecurity, allowedAttachments, quarantinedAttachments);
        String capabilityZone = resolveCapabilityZone(capability, toolScope);
        String trustZone = resolveTrustZone(userProfile, sigiloSensitive, blockedSources, quarantinedAttachments, doctorBlocked, bootstrapBlocked, suppressionLocked, suppressionEscalated, capabilityZone, allowlistedSources, allowedAttachments);
        String status = switch (trustZone) {
            case "CRITICAL" -> "LOCKED";
            case "SIGILOSA" -> "ESCALATED";
            case "INSTITUTIONAL" -> "ENFORCED";
            default -> "NOT_REQUIRED";
        };
        String trustZoneMode = switch (trustZone) {
            case "CRITICAL" -> "SOVEREIGN_HARD_LOCK";
            case "SIGILOSA" -> "SOVEREIGN_STEP_UP_GATED";
            case "INSTITUTIONAL" -> "SOVEREIGN_BOUNDARY_ENFORCED";
            default -> "PUBLIC_BOUNDARY";
        };
        boolean sovereignBoundaryRequired = !"PUBLIC".equals(trustZone);
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>(toolScope == null || toolScope.blockedToolIds() == null ? List.of() : toolScope.blockedToolIds());
        LinkedHashSet<String> elevatedStepUpToolIds = new LinkedHashSet<>(toolScope == null || toolScope.stepUpToolIds() == null ? List.of() : toolScope.stepUpToolIds());
        LinkedHashSet<String> allowedToolIds = new LinkedHashSet<>(toolScope == null || toolScope.allowedToolIds() == null ? List.of() : toolScope.allowedToolIds());
        LinkedHashSet<String> sensitiveToolIds = new LinkedHashSet<>();
        allowedToolIds.stream().filter(this::isSensitiveTool).forEach(sensitiveToolIds::add);
        elevatedStepUpToolIds.stream().filter(this::isSensitiveTool).forEach(sensitiveToolIds::add);
        if ("CRITICAL".equals(trustZone)) {
            blockedToolIds.addAll(allowedToolIds);
            blockedToolIds.addAll(elevatedStepUpToolIds);
            elevatedStepUpToolIds.clear();
        } else if ("SIGILOSA".equals(trustZone)) {
            blockedToolIds.addAll(sensitiveToolIds);
            allowedToolIds.stream().filter(id -> !blockedToolIds.contains(id)).forEach(elevatedStepUpToolIds::add);
        } else if ("INSTITUTIONAL".equals(trustZone) && "MUTATING".equals(capabilityZone)) {
            elevatedStepUpToolIds.addAll(sensitiveToolIds.isEmpty() ? allowedToolIds : sensitiveToolIds);
        }
        List<String> unmetRequirements = new ArrayList<>();
        if (blockedSources) {
            unmetRequirements.add("OFFICIAL_SOURCE_BOUNDARY_NOT_CLEARED");
        }
        if (quarantinedAttachments) {
            unmetRequirements.add("ATTACHMENT_BOUNDARY_NOT_CLEARED");
        }
        if (sigiloSensitive) {
            unmetRequirements.add("SIGILO_BOUNDARY_ACTIVE");
        }
        if (doctorBlocked) {
            unmetRequirements.add("SESSION_DOCTOR_BLOCK_ACTIVE");
        }
        if (bootstrapBlocked) {
            unmetRequirements.add("SESSION_BOOTSTRAP_BLOCK_ACTIVE");
        }
        if (suppressionLocked || suppressionEscalated) {
            unmetRequirements.add("CAPABILITY_SUPPRESSION_BOUNDARY_ACTIVE");
        }
        List<String> reasons = new ArrayList<>();
        if ("PUBLIC".equals(trustZone)) {
            reasons.add("A capability permaneceu em trust zone pública porque as fontes, anexos e o sigilo não exigiram reforço soberano adicional neste turno.");
        } else {
            reasons.add("A capability entrou em trust zone soberana para impedir mistura indevida entre contexto público, institucional, sigiloso e crítico.");
            if (blockedSources) {
                reasons.add("Fontes fora da allowlist oficial deslocaram a trilha para fronteira soberana reforçada antes de qualquer expansão contextual.");
            }
            if (quarantinedAttachments) {
                reasons.add("Anexos em quarentena impediram promoção ingênua de conteúdo para RAG, grounding ou tool use sensível.");
            }
            if (sigiloSensitive) {
                reasons.add("Sigilo sensível exigiu trust zone reforçada para capability, fonte e anexo antes de qualquer reaproveitamento mutável.");
            }
            if ("CRITICAL".equals(trustZone)) {
                reasons.add("A combinação entre sigilo, superfície bloqueada ou supressão crítica travou a capability em zona crítica soberana.");
            }
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("userProfile", userProfile);
        diagnostics.put("sourceZone", sourceZone);
        diagnostics.put("attachmentZone", attachmentZone);
        diagnostics.put("capabilityZone", capabilityZone);
        diagnostics.put("trustZone", trustZone);
        diagnostics.put("status", status);
        diagnostics.put("sigiloSensitive", sigiloSensitive);
        diagnostics.put("blockedSources", blockedSources);
        diagnostics.put("quarantinedAttachments", quarantinedAttachments);
        diagnostics.put("allowlistedSources", allowlistedSources);
        diagnostics.put("allowedAttachments", allowedAttachments);
        diagnostics.put("doctorBlocked", doctorBlocked);
        diagnostics.put("bootstrapBlocked", bootstrapBlocked);
        diagnostics.put("suppressionLocked", suppressionLocked);
        diagnostics.put("suppressionEscalated", suppressionEscalated);
        diagnostics.put("trustZoneMode", trustZoneMode);
        diagnostics.put("sovereignBoundaryRequired", sovereignBoundaryRequired);
        return new LegalAiConversationTrustZoneSnapshot(
                status,
                trustZone,
                sovereignBoundaryRequired,
                processScoped,
                sourceZone,
                attachmentZone,
                capabilityZone,
                trustZoneMode,
                List.copyOf(blockedToolIds),
                List.copyOf(elevatedStepUpToolIds),
                List.copyOf(unmetRequirements),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private String resolveSourceZone(LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                     boolean allowlistedSources,
                                     boolean blockedSources,
                                     String userProfile) {
        if (blockedSources) {
            return "BLOCKED_EXTERNAL";
        }
        if (allowlistedSources) {
            return "OFFICIAL_JUDICIAL";
        }
        if ("CIDADAO".equals(userProfile)) {
            return "PUBLIC_INTERNAL";
        }
        return "INTERNAL_ONLY";
    }

    private String resolveAttachmentZone(LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                         boolean allowedAttachments,
                                         boolean quarantinedAttachments) {
        if (quarantinedAttachments) {
            return "QUARANTINED";
        }
        if (allowedAttachments) {
            return "CONTROLLED";
        }
        return "NONE";
    }

    private String resolveCapabilityZone(String capability, LegalAiConversationToolScopeSnapshot toolScope) {
        String normalizedCapability = normalize(capability);
        boolean mutating = normalizedCapability != null
                && (normalizedCapability.contains("DRAFT")
                || normalizedCapability.contains("WRITE")
                || normalizedCapability.contains("DECISAO")
                || normalizedCapability.contains("DESPACHO")
                || normalizedCapability.contains("PETIC")
                || normalizedCapability.contains("RECURSAL"));
        if (!mutating && toolScope != null && toolScope.stepUpToolIds() != null && !toolScope.stepUpToolIds().isEmpty()) {
            mutating = toolScope.stepUpToolIds().stream().anyMatch(this::isSensitiveTool);
        }
        return mutating ? "MUTATING" : "ANALYTICAL";
    }

    private String resolveTrustZone(String userProfile,
                                    boolean sigiloSensitive,
                                    boolean blockedSources,
                                    boolean quarantinedAttachments,
                                    boolean doctorBlocked,
                                    boolean bootstrapBlocked,
                                    boolean suppressionLocked,
                                    boolean suppressionEscalated,
                                    String capabilityZone,
                                    boolean allowlistedSources,
                                    boolean allowedAttachments) {
        if (suppressionLocked || doctorBlocked || bootstrapBlocked || (sigiloSensitive && (blockedSources || quarantinedAttachments))) {
            return "CRITICAL";
        }
        if (sigiloSensitive || blockedSources || quarantinedAttachments || suppressionEscalated) {
            return "SIGILOSA";
        }
        if ("MUTATING".equals(capabilityZone) || allowlistedSources || allowedAttachments || !"CIDADAO".equals(userProfile)) {
            return "INSTITUTIONAL";
        }
        return "PUBLIC";
    }

    private boolean isSensitiveTool(String toolId) {
        String normalized = normalize(toolId);
        return normalized != null && (normalized.contains("WRITE")
                || normalized.contains("DRAFT")
                || normalized.contains("PETIC")
                || normalized.contains("DECISAO")
                || normalized.contains("DESPACHO")
                || normalized.contains("PROTOCOLO")
                || normalized.contains("ASSIN"));
    }

    private String contextValue(LegalAiConversationRequest request, String key) {
        if (request == null || request.context() == null || key == null) {
            return null;
        }
        Object value = request.context().get(key);
        return value == null ? null : String.valueOf(value);
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
