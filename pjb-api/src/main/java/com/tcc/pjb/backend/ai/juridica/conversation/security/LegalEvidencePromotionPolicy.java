package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAttachmentProvenanceClassifier.AttachmentProvenanceDecision;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDraftPromotionFence.PromotionFenceDecision;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceTrustClassifier.EvidenceTrustDecision;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalEvidencePromotionPolicy {

    private final LegalGroundingPromotionFence groundingPromotionFence;
    private final LegalDraftPromotionFence draftPromotionFence;

    public LegalEvidencePromotionPolicy(LegalGroundingPromotionFence groundingPromotionFence,
                                        LegalDraftPromotionFence draftPromotionFence) {
        this.groundingPromotionFence = Objects.requireNonNull(groundingPromotionFence, "groundingPromotionFence");
        this.draftPromotionFence = Objects.requireNonNull(draftPromotionFence, "draftPromotionFence");
    }

    public EvidencePromotionDecision resolve(LegalAiConversationRequest request,
                                             String capability,
                                             String version,
                                             LegalAiConversationTrustZoneSnapshot trustZone,
                                             LegalAiConversationToolScopeSnapshot toolScope,
                                             EvidenceTrustDecision sourceDecision,
                                             AttachmentProvenanceDecision attachmentDecision) {
        String sourceTier = sourceDecision == null ? "NO_EVIDENCE" : sourceDecision.tier();
        String attachmentTier = attachmentDecision == null ? "NO_EVIDENCE" : attachmentDecision.tier();
        String effectiveTier = resolveEffectiveTier(sourceTier, attachmentTier);
        var groundingDecision = groundingPromotionFence.inspect(effectiveTier, sourceTier, trustZone, toolScope);
        PromotionFenceDecision draftDecision = draftPromotionFence.inspect(request, capability, effectiveTier, trustZone, toolScope);
        String ragPromotionStatus = resolveRagPromotionStatus(effectiveTier, trustZone, groundingDecision.status());
        String suggestionPromotionStatus = resolveSuggestionPromotionStatus(effectiveTier, trustZone, draftDecision.status());
        String capabilityRecoveryPromotionStatus = resolveCapabilityRecoveryPromotionStatus(effectiveTier, trustZone, groundingDecision.status(), draftDecision.status());
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>();
        LinkedHashSet<String> elevatedStepUpToolIds = new LinkedHashSet<>();
        blockedToolIds.addAll(groundingDecision.blockedToolIds());
        blockedToolIds.addAll(draftDecision.blockedToolIds());
        elevatedStepUpToolIds.addAll(groundingDecision.elevatedStepUpToolIds());
        elevatedStepUpToolIds.addAll(draftDecision.elevatedStepUpToolIds());
        List<String> capabilityRecoveryCandidateToolIds = recoveryCandidateToolIds(toolScope);
        if ("BLOCKED".equalsIgnoreCase(capabilityRecoveryPromotionStatus)) {
            blockedToolIds.addAll(capabilityRecoveryCandidateToolIds);
            elevatedStepUpToolIds.removeAll(capabilityRecoveryCandidateToolIds);
        } else if ("STEP_UP_REQUIRED".equalsIgnoreCase(capabilityRecoveryPromotionStatus)) {
            elevatedStepUpToolIds.addAll(capabilityRecoveryCandidateToolIds);
        }
        List<String> reasons = new ArrayList<>();
        if (sourceDecision != null) {
            reasons.addAll(sourceDecision.reasons());
        }
        if (attachmentDecision != null) {
            reasons.addAll(attachmentDecision.reasons());
        }
        reasons.addAll(groundingDecision.reasons());
        reasons.addAll(draftDecision.reasons());
        reasons.add("RAG promotion=" + ragPromotionStatus + " para tier efetivo " + effectiveTier + '.');
        reasons.add("Suggestion flow=" + suggestionPromotionStatus + " para tier efetivo " + effectiveTier + '.');
        reasons.add("Capability recovery promotion=" + capabilityRecoveryPromotionStatus + " para tier efetivo " + effectiveTier + '.');
        List<String> unmetRequirements = new ArrayList<>();
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveTier)) {
            unmetRequirements.add("UNTRUSTED_EVIDENCE_PRESENT");
        }
        if ("DERIVED_DOCUMENT".equalsIgnoreCase(effectiveTier)) {
            unmetRequirements.add("DERIVED_EVIDENCE_REQUIRES_SOVEREIGN_CONFIRMATION");
        }
        if ("NO_EVIDENCE".equalsIgnoreCase(sourceTier) && "NO_EVIDENCE".equalsIgnoreCase(attachmentTier)) {
            unmetRequirements.add("EVIDENCE_CHAIN_NOT_MATERIALIZED");
        }
        if (trustZone != null && trustZone.sovereignBoundaryRequired() && !"OFFICIAL_DOCUMENT".equalsIgnoreCase(sourceTier)) {
            unmetRequirements.add("OFFICIAL_SOURCE_ANCHOR_MISSING");
        }
        if ("BLOCKED".equalsIgnoreCase(groundingDecision.status())) {
            unmetRequirements.add("GROUNDING_PROMOTION_BLOCKED");
        }
        if ("BLOCKED".equalsIgnoreCase(draftDecision.status())) {
            unmetRequirements.add("DRAFT_PROMOTION_BLOCKED");
        }
        if ("BLOCKED".equalsIgnoreCase(capabilityRecoveryPromotionStatus)) {
            unmetRequirements.add("CAPABILITY_RECOVERY_PROMOTION_BLOCKED");
        }
        String status = resolveStatus(effectiveTier, trustZone, ragPromotionStatus, groundingDecision.status(), draftDecision.status(), suggestionPromotionStatus, capabilityRecoveryPromotionStatus);
        String sovereignProvenanceMode = resolveMode(effectiveTier, status, trustZone);
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("effectiveEvidenceTier", effectiveTier);
        diagnostics.put("sourceEvidenceTier", sourceTier);
        diagnostics.put("attachmentEvidenceTier", attachmentTier);
        diagnostics.put("sovereignProvenanceMode", sovereignProvenanceMode);
        diagnostics.put("ragPromotionStatus", ragPromotionStatus);
        diagnostics.put("groundingPromotionStatus", groundingDecision.status());
        diagnostics.put("draftPromotionStatus", draftDecision.status());
        diagnostics.put("suggestionPromotionStatus", suggestionPromotionStatus);
        diagnostics.put("capabilityRecoveryPromotionStatus", capabilityRecoveryPromotionStatus);
        diagnostics.put("capabilityRecoveryCandidateToolIds", capabilityRecoveryCandidateToolIds);
        diagnostics.put("officialEvidenceIds", union(sourceDecision == null ? List.of() : sourceDecision.officialEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.officialEvidenceIds()));
        diagnostics.put("institutionalControlledEvidenceIds", union(sourceDecision == null ? List.of() : sourceDecision.institutionalControlledEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.institutionalControlledEvidenceIds()));
        diagnostics.put("derivedEvidenceIds", union(sourceDecision == null ? List.of() : sourceDecision.derivedEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.derivedEvidenceIds()));
        diagnostics.put("untrustedEvidenceIds", union(sourceDecision == null ? List.of() : sourceDecision.untrustedEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.untrustedEvidenceIds()));
        diagnostics.put("status", status);
        return new EvidencePromotionDecision(
                status,
                effectiveTier,
                sourceTier,
                attachmentTier,
                sovereignProvenanceMode,
                ragPromotionStatus,
                groundingDecision.status(),
                draftDecision.status(),
                suggestionPromotionStatus,
                capabilityRecoveryPromotionStatus,
                union(sourceDecision == null ? List.of() : sourceDecision.officialEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.officialEvidenceIds()),
                union(sourceDecision == null ? List.of() : sourceDecision.institutionalControlledEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.institutionalControlledEvidenceIds()),
                union(sourceDecision == null ? List.of() : sourceDecision.derivedEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.derivedEvidenceIds()),
                union(sourceDecision == null ? List.of() : sourceDecision.untrustedEvidenceIds(), attachmentDecision == null ? List.of() : attachmentDecision.untrustedEvidenceIds()),
                List.copyOf(blockedToolIds),
                List.copyOf(elevatedStepUpToolIds),
                List.copyOf(unmetRequirements),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private List<String> recoveryCandidateToolIds(LegalAiConversationToolScopeSnapshot toolScope) {
        if (toolScope == null || toolScope.diagnostics() == null) {
            return List.of();
        }
        Object value = toolScope.diagnostics().get("capabilityRecoveryCandidateToolIds");
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private List<String> union(List<String> first, List<String> second) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (first != null) {
            out.addAll(first);
        }
        if (second != null) {
            out.addAll(second);
        }
        return List.copyOf(out);
    }

    private String resolveEffectiveTier(String sourceTier, String attachmentTier) {
        return rank(sourceTier) >= rank(attachmentTier) ? sourceTier : attachmentTier;
    }

    private int rank(String tier) {
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(tier)) {
            return 4;
        }
        if ("DERIVED_DOCUMENT".equalsIgnoreCase(tier)) {
            return 3;
        }
        if ("INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(tier)) {
            return 2;
        }
        if ("OFFICIAL_DOCUMENT".equalsIgnoreCase(tier)) {
            return 1;
        }
        return 0;
    }

    private String resolveRagPromotionStatus(String effectiveTier,
                                             LegalAiConversationTrustZoneSnapshot trustZone,
                                             String groundingStatus) {
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveTier)
                || trustZone != null && "LOCKED".equalsIgnoreCase(trustZone.status())) {
            return "BLOCKED";
        }
        if ("STEP_UP_REQUIRED".equalsIgnoreCase(groundingStatus)
                || "DERIVED_DOCUMENT".equalsIgnoreCase(effectiveTier)
                || trustZone != null && "SIGILOSA".equalsIgnoreCase(trustZone.trustZone())) {
            return "STEP_UP_REQUIRED";
        }
        if ("INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(effectiveTier)
                || trustZone != null && trustZone.sovereignBoundaryRequired()) {
            return "MONITORED";
        }
        return "PROMOTED";
    }

    private String resolveSuggestionPromotionStatus(String effectiveTier,
                                                    LegalAiConversationTrustZoneSnapshot trustZone,
                                                    String draftStatus) {
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveTier)
                || "BLOCKED".equalsIgnoreCase(draftStatus)) {
            return "BLOCKED";
        }
        if ("DERIVED_DOCUMENT".equalsIgnoreCase(effectiveTier)) {
            return "MONITORED";
        }
        if (trustZone != null && trustZone.sovereignBoundaryRequired()) {
            return "STEP_UP_REQUIRED";
        }
        return "PROMOTED";
    }

    private String resolveCapabilityRecoveryPromotionStatus(String effectiveTier,
                                                            LegalAiConversationTrustZoneSnapshot trustZone,
                                                            String groundingStatus,
                                                            String draftStatus) {
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveTier)
                || "BLOCKED".equalsIgnoreCase(groundingStatus)
                || "BLOCKED".equalsIgnoreCase(draftStatus)
                || trustZone != null && "LOCKED".equalsIgnoreCase(trustZone.status())) {
            return "BLOCKED";
        }
        if (!"OFFICIAL_DOCUMENT".equalsIgnoreCase(effectiveTier)
                || trustZone != null && trustZone.sovereignBoundaryRequired()) {
            return "STEP_UP_REQUIRED";
        }
        return "PROMOTED";
    }

    private String resolveStatus(String effectiveTier,
                                 LegalAiConversationTrustZoneSnapshot trustZone,
                                 String ragPromotionStatus,
                                 String groundingPromotionStatus,
                                 String draftPromotionStatus,
                                 String suggestionPromotionStatus,
                                 String capabilityRecoveryPromotionStatus) {
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveTier)
                || "BLOCKED".equalsIgnoreCase(groundingPromotionStatus)
                || "BLOCKED".equalsIgnoreCase(draftPromotionStatus)
                || "BLOCKED".equalsIgnoreCase(capabilityRecoveryPromotionStatus)
                || trustZone != null && "LOCKED".equalsIgnoreCase(trustZone.status())) {
            return "LOCKED";
        }
        if ("STEP_UP_REQUIRED".equalsIgnoreCase(ragPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(groundingPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(draftPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(suggestionPromotionStatus)
                || "STEP_UP_REQUIRED".equalsIgnoreCase(capabilityRecoveryPromotionStatus)) {
            return "ESCALATED";
        }
        if ("MONITORED".equalsIgnoreCase(ragPromotionStatus)
                || "MONITORED".equalsIgnoreCase(suggestionPromotionStatus)
                || trustZone != null && trustZone.sovereignBoundaryRequired()) {
            return "ENFORCED";
        }
        return "NOT_REQUIRED";
    }

    private String resolveMode(String effectiveTier,
                               String status,
                               LegalAiConversationTrustZoneSnapshot trustZone) {
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(effectiveTier) || "LOCKED".equalsIgnoreCase(status)) {
            return "SOVEREIGN_PROVENANCE_HARD_LOCK";
        }
        if ("DERIVED_DOCUMENT".equalsIgnoreCase(effectiveTier)) {
            return "SOVEREIGN_DERIVED_CHAIN_GATED";
        }
        if ("INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(effectiveTier)) {
            return trustZone != null && trustZone.sovereignBoundaryRequired()
                    ? "SOVEREIGN_INSTITUTIONAL_CHAIN"
                    : "INSTITUTIONAL_CHAIN";
        }
        if ("OFFICIAL_DOCUMENT".equalsIgnoreCase(effectiveTier)) {
            return trustZone != null && trustZone.sovereignBoundaryRequired()
                    ? "SOVEREIGN_OFFICIAL_CHAIN"
                    : "OFFICIAL_CHAIN";
        }
        return "NO_EVIDENCE_CHAIN";
    }

    public record EvidencePromotionDecision(
            String status,
            String effectiveEvidenceTier,
            String sourceEvidenceTier,
            String attachmentEvidenceTier,
            String sovereignProvenanceMode,
            String ragPromotionStatus,
            String groundingPromotionStatus,
            String draftPromotionStatus,
            String suggestionPromotionStatus,
            String capabilityRecoveryPromotionStatus,
            List<String> officialEvidenceIds,
            List<String> institutionalControlledEvidenceIds,
            List<String> derivedEvidenceIds,
            List<String> untrustedEvidenceIds,
            List<String> blockedToolIds,
            List<String> elevatedStepUpToolIds,
            List<String> unmetRequirements,
            List<String> reasons,
            Map<String, Object> diagnostics
    ) {
        public EvidencePromotionDecision {
            officialEvidenceIds = officialEvidenceIds == null ? List.of() : List.copyOf(officialEvidenceIds);
            institutionalControlledEvidenceIds = institutionalControlledEvidenceIds == null ? List.of() : List.copyOf(institutionalControlledEvidenceIds);
            derivedEvidenceIds = derivedEvidenceIds == null ? List.of() : List.copyOf(derivedEvidenceIds);
            untrustedEvidenceIds = untrustedEvidenceIds == null ? List.of() : List.copyOf(untrustedEvidenceIds);
            blockedToolIds = blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds);
            elevatedStepUpToolIds = elevatedStepUpToolIds == null ? List.of() : List.copyOf(elevatedStepUpToolIds);
            unmetRequirements = unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements);
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            diagnostics = diagnostics == null ? Map.of() : ImmutableViewSupport.map(diagnostics);
        }
    }
}
