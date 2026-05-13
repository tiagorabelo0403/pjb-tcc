package com.tcc.pjb.backend.service.intelligence.surface;

import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeCommentaryTextCatalogService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.surface.LegalAiStructuredSurfaceEvidenceBundle;
import com.tcc.pjb.backend.model.dto.ai.legal.surface.LegalAiStructuredSurfaceGovernanceSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class LegalAiStructuredSurfaceEvidenceAssemblerService {

    private final LegalKnowledgeCommentaryTextCatalogService commentaryTextCatalogService;

    public LegalAiStructuredSurfaceEvidenceAssemblerService(LegalKnowledgeCommentaryTextCatalogService commentaryTextCatalogService) {
        this.commentaryTextCatalogService = Objects.requireNonNull(commentaryTextCatalogService, "commentaryTextCatalogService");
    }

    public LegalAiStructuredSurfaceEvidenceBundle assembleForDraft(LegalAiStructuredSurfaceGovernanceSnapshot governance) {
        return assemble(governance, commentaryTextCatalogService.draftSurfaceCode(), descriptor -> descriptor.promotedForDraft());
    }

    public LegalAiStructuredSurfaceEvidenceBundle assembleForGrounding(LegalAiStructuredSurfaceGovernanceSnapshot governance) {
        return assemble(governance, commentaryTextCatalogService.groundingSurfaceCode(), descriptor -> descriptor.promotedForGrounding());
    }

    private LegalAiStructuredSurfaceEvidenceBundle assemble(LegalAiStructuredSurfaceGovernanceSnapshot governance,
                                                            String surfaceCode,
                                                            Predicate<LegalAiConversationEvidenceDescriptor> promotionSelector) {
        LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance = governance == null ? null : governance.evidenceProvenance();
        List<LegalAiConversationEvidenceDescriptor> descriptors = evidenceProvenance == null || evidenceProvenance.evidenceDescriptors() == null
                ? List.of()
                : evidenceProvenance.evidenceDescriptors();
        LinkedHashMap<String, LegalAiConversationEvidenceDescriptor> promotedDescriptors = new LinkedHashMap<>();
        descriptors.stream()
                .filter(Objects::nonNull)
                .filter(promotionSelector)
                .forEach(descriptor -> promotedDescriptors.put(descriptor.evidenceId(), descriptor));
        String promotionStatus = resolvePromotionStatus(surfaceCode, evidenceProvenance);
        boolean anchored = commentaryTextCatalogService.promotedStatus().equalsIgnoreCase(promotionStatus) && !promotedDescriptors.isEmpty();
        return new LegalAiStructuredSurfaceEvidenceBundle(
                surfaceCode,
                promotionStatus,
                anchored,
                List.copyOf(promotedDescriptors.keySet()),
                List.copyOf(promotedDescriptors.values()),
                unmetRequirements(surfaceCode, evidenceProvenance),
                reasons(surfaceCode, evidenceProvenance, anchored)
        );
    }

    private String resolvePromotionStatus(String surfaceCode,
                                          LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance) {
        if (evidenceProvenance == null) {
            return commentaryTextCatalogService.blockedStatus();
        }
        if (commentaryTextCatalogService.groundingSurfaceCode().equals(surfaceCode)) {
            return valueOrDefault(evidenceProvenance.groundingPromotionStatus(), commentaryTextCatalogService.blockedStatus());
        }
        if (commentaryTextCatalogService.draftSurfaceCode().equals(surfaceCode)) {
            return valueOrDefault(evidenceProvenance.draftPromotionStatus(), commentaryTextCatalogService.blockedStatus());
        }
        return valueOrDefault(evidenceProvenance.status(), commentaryTextCatalogService.blockedStatus());
    }

    private List<String> unmetRequirements(String surfaceCode,
                                           LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance) {
        if (evidenceProvenance == null || evidenceProvenance.unmetRequirements() == null) {
            return List.of();
        }
        return evidenceProvenance.unmetRequirements().stream()
                .filter(Objects::nonNull)
                .filter(item -> appliesToSurface(surfaceCode, item))
                .toList();
    }

    private boolean appliesToSurface(String surfaceCode, String requirement) {
        if (requirement == null || requirement.isBlank()) {
            return false;
        }
        if (commentaryTextCatalogService.groundingSurfaceCode().equals(surfaceCode)) {
            return !requirement.startsWith(commentaryTextCatalogService.draftRequirementPrefix());
        }
        if (commentaryTextCatalogService.draftSurfaceCode().equals(surfaceCode)) {
            return !requirement.startsWith(commentaryTextCatalogService.groundingRequirementPrefix());
        }
        return true;
    }

    private List<String> reasons(String surfaceCode,
                                 LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance,
                                 boolean anchored) {
        LinkedHashMap<String, String> reasons = new LinkedHashMap<>();
        if (evidenceProvenance != null && evidenceProvenance.reasons() != null) {
            evidenceProvenance.reasons().stream()
                    .filter(Objects::nonNull)
                    .filter(reason -> appliesToSurface(surfaceCode, reason))
                    .forEach(reason -> reasons.put(reason, reason));
        }
        if (!anchored) {
            String reason = commentaryTextCatalogService.surfacePromotionNotAnchoredReason();
            reasons.put(reason, reason);
        }
        return List.copyOf(reasons.values());
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public Map<String, Object> toSafeguards(LegalAiStructuredSurfaceEvidenceBundle bundle) {
        if (bundle == null) {
            return Map.of();
        }
        return bundle.asMap();
    }
}
