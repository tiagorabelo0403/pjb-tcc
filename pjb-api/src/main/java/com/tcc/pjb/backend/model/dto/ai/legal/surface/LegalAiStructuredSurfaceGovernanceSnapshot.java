package com.tcc.pjb.backend.model.dto.ai.legal.surface;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSanitizationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public record LegalAiStructuredSurfaceGovernanceSnapshot(
        LegalAiConversationRequest effectiveRequest,
        LegalAiConversationSanitizationSnapshot sanitization,
        LegalAiConversationDocumentSecuritySnapshot documentSecurity,
        LegalAiConversationToolScopeSnapshot toolScope,
        LegalAiConversationTrustZoneSnapshot trustZone,
        LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance
) {

    public boolean groundingBlocked() {
        return evidenceProvenance != null && "BLOCKED".equalsIgnoreCase(evidenceProvenance.groundingPromotionStatus());
    }

    public boolean groundingStepUpRequired() {
        return evidenceProvenance != null && "STEP_UP_REQUIRED".equalsIgnoreCase(evidenceProvenance.groundingPromotionStatus());
    }

    public boolean draftBlocked() {
        return evidenceProvenance != null && "BLOCKED".equalsIgnoreCase(evidenceProvenance.draftPromotionStatus());
    }

    public boolean draftStepUpRequired() {
        return evidenceProvenance != null && "STEP_UP_REQUIRED".equalsIgnoreCase(evidenceProvenance.draftPromotionStatus());
    }

    public String surfaceStatus() {
        if (evidenceProvenance != null) {
            if ("BLOCKED".equalsIgnoreCase(evidenceProvenance.draftPromotionStatus()) || "BLOCKED".equalsIgnoreCase(evidenceProvenance.status())) {
                return "LOCKED";
            }
            if ("PROMOTED".equalsIgnoreCase(evidenceProvenance.groundingPromotionStatus()) && !groundingBlocked() && !groundingStepUpRequired() && !draftBlocked() && !draftStepUpRequired()) {
                return "NOT_REQUIRED";
            }
            if (evidenceProvenance.status() != null) {
                return evidenceProvenance.status();
            }
        }
        return trustZone == null ? "NOT_REQUIRED" : trustZone.status();
    }

    public List<String> nextSteps() {
        LinkedHashSet<String> steps = new LinkedHashSet<>();
        if (sanitization != null && sanitization.alerts() != null) {
            steps.addAll(sanitization.alerts());
        }
        if (documentSecurity != null && documentSecurity.alerts() != null) {
            steps.addAll(documentSecurity.alerts());
        }
        if (trustZone != null && trustZone.reasons() != null) {
            steps.addAll(trustZone.reasons());
        }
        if (evidenceProvenance != null && evidenceProvenance.reasons() != null) {
            steps.addAll(evidenceProvenance.reasons());
        }
        if (trustZone != null && trustZone.unmetRequirements() != null) {
            trustZone.unmetRequirements().forEach(item -> steps.add("Boundary pendente: " + item));
        }
        if (evidenceProvenance != null && evidenceProvenance.unmetRequirements() != null) {
            evidenceProvenance.unmetRequirements().forEach(item -> steps.add("Proveniência pendente: " + item));
        }
        return List.copyOf(steps);
    }

    public Map<String, Object> safeguards() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfPresent(out, "surfaceStatus", surfaceStatus());
        putIfPresent(out, "sanitizationStatus", sanitization == null ? null : sanitization.status());
        out.put("promptInjectionDetected", sanitization != null && sanitization.promptInjectionDetected());
        putIfPresent(out, "documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        putIfPresent(out, "trustZoneStatus", trustZone == null ? null : trustZone.status());
        putIfPresent(out, "trustZone", trustZone == null ? null : trustZone.trustZone());
        putIfPresent(out, "trustZoneMode", trustZone == null ? null : trustZone.trustZoneMode());
        out.put("sovereignBoundaryRequired", trustZone != null && trustZone.sovereignBoundaryRequired());
        putIfPresent(out, "evidenceProvenanceStatus", evidenceProvenance == null ? null : evidenceProvenance.status());
        putIfPresent(out, "evidenceProvenanceTier", evidenceProvenance == null ? null : evidenceProvenance.effectiveEvidenceTier());
        putIfPresent(out, "evidenceSourceTier", evidenceProvenance == null ? null : evidenceProvenance.sourceEvidenceTier());
        putIfPresent(out, "evidenceAttachmentTier", evidenceProvenance == null ? null : evidenceProvenance.attachmentEvidenceTier());
        putIfPresent(out, "evidenceProvenanceMode", evidenceProvenance == null ? null : evidenceProvenance.sovereignProvenanceMode());
        putIfPresent(out, "groundingPromotionStatus", evidenceProvenance == null ? null : evidenceProvenance.groundingPromotionStatus());
        putIfPresent(out, "draftPromotionStatus", evidenceProvenance == null ? null : evidenceProvenance.draftPromotionStatus());
        putIfPresent(out, "ragPromotionStatus", evidenceProvenance == null ? null : evidenceProvenance.ragPromotionStatus());
        putIfPresent(out, "suggestionPromotionStatus", evidenceProvenance == null ? null : evidenceProvenance.suggestionPromotionStatus());
        putIfPresent(out, "capabilityRecoveryPromotionStatus", evidenceProvenance == null ? null : evidenceProvenance.capabilityRecoveryPromotionStatus());
        out.put("blockedToolIds", collectBlockedTools());
        out.put("stepUpToolIds", collectStepUpTools());
        out.put("officialEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.officialEvidenceIds());
        out.put("institutionalControlledEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.institutionalControlledEvidenceIds());
        out.put("derivedEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.derivedEvidenceIds());
        out.put("untrustedEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.untrustedEvidenceIds());
        out.put("evidenceDescriptors", evidenceProvenance == null || evidenceProvenance.evidenceDescriptors() == null ? List.of() : evidenceProvenance.evidenceDescriptors().stream().map(com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceDescriptor::asMap).toList());
        out.put("promotedRagEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.promotedRagEvidenceIds());
        out.put("promotedGroundingEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.promotedGroundingEvidenceIds());
        out.put("promotedDraftEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.promotedDraftEvidenceIds());
        out.put("promotedSuggestionEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.promotedSuggestionEvidenceIds());
        out.put("promotedCapabilityRecoveryEvidenceIds", evidenceProvenance == null ? List.of() : evidenceProvenance.promotedCapabilityRecoveryEvidenceIds());
        return Collections.unmodifiableMap(out);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        target.put(key, value);
    }

    private List<String> collectBlockedTools() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (toolScope != null && toolScope.blockedToolIds() != null) {
            out.addAll(toolScope.blockedToolIds());
        }
        if (trustZone != null && trustZone.blockedToolIds() != null) {
            out.addAll(trustZone.blockedToolIds());
        }
        if (evidenceProvenance != null && evidenceProvenance.blockedToolIds() != null) {
            out.addAll(evidenceProvenance.blockedToolIds());
        }
        return List.copyOf(out);
    }

    private List<String> collectStepUpTools() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (toolScope != null && toolScope.stepUpToolIds() != null) {
            out.addAll(toolScope.stepUpToolIds());
        }
        if (trustZone != null && trustZone.elevatedStepUpToolIds() != null) {
            out.addAll(trustZone.elevatedStepUpToolIds());
        }
        if (evidenceProvenance != null && evidenceProvenance.elevatedStepUpToolIds() != null) {
            out.addAll(evidenceProvenance.elevatedStepUpToolIds());
        }
        return List.copyOf(out);
    }
}
