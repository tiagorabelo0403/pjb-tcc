package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationEvidenceProvenanceSnapshot(
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
        List<LegalAiConversationEvidenceDescriptor> evidenceDescriptors,
        List<String> promotedRagEvidenceIds,
        List<String> promotedGroundingEvidenceIds,
        List<String> promotedDraftEvidenceIds,
        List<String> promotedSuggestionEvidenceIds,
        List<String> promotedCapabilityRecoveryEvidenceIds,
        List<String> blockedToolIds,
        List<String> elevatedStepUpToolIds,
        List<String> unmetRequirements,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("effectiveEvidenceTier", effectiveEvidenceTier);
        out.put("sourceEvidenceTier", sourceEvidenceTier);
        out.put("attachmentEvidenceTier", attachmentEvidenceTier);
        out.put("sovereignProvenanceMode", sovereignProvenanceMode);
        out.put("ragPromotionStatus", ragPromotionStatus);
        out.put("groundingPromotionStatus", groundingPromotionStatus);
        out.put("draftPromotionStatus", draftPromotionStatus);
        out.put("suggestionPromotionStatus", suggestionPromotionStatus);
        out.put("capabilityRecoveryPromotionStatus", capabilityRecoveryPromotionStatus);
        out.put("officialEvidenceIds", officialEvidenceIds == null ? List.of() : List.copyOf(officialEvidenceIds));
        out.put("institutionalControlledEvidenceIds", institutionalControlledEvidenceIds == null ? List.of() : List.copyOf(institutionalControlledEvidenceIds));
        out.put("derivedEvidenceIds", derivedEvidenceIds == null ? List.of() : List.copyOf(derivedEvidenceIds));
        out.put("untrustedEvidenceIds", untrustedEvidenceIds == null ? List.of() : List.copyOf(untrustedEvidenceIds));
        out.put("evidenceDescriptors", evidenceDescriptors == null ? List.of() : evidenceDescriptors.stream().map(LegalAiConversationEvidenceDescriptor::asMap).toList());
        out.put("promotedRagEvidenceIds", promotedRagEvidenceIds == null ? List.of() : List.copyOf(promotedRagEvidenceIds));
        out.put("promotedGroundingEvidenceIds", promotedGroundingEvidenceIds == null ? List.of() : List.copyOf(promotedGroundingEvidenceIds));
        out.put("promotedDraftEvidenceIds", promotedDraftEvidenceIds == null ? List.of() : List.copyOf(promotedDraftEvidenceIds));
        out.put("promotedSuggestionEvidenceIds", promotedSuggestionEvidenceIds == null ? List.of() : List.copyOf(promotedSuggestionEvidenceIds));
        out.put("promotedCapabilityRecoveryEvidenceIds", promotedCapabilityRecoveryEvidenceIds == null ? List.of() : List.copyOf(promotedCapabilityRecoveryEvidenceIds));
        out.put("blockedToolIds", blockedToolIds == null ? List.of() : List.copyOf(blockedToolIds));
        out.put("elevatedStepUpToolIds", elevatedStepUpToolIds == null ? List.of() : List.copyOf(elevatedStepUpToolIds));
        out.put("unmetRequirements", unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
