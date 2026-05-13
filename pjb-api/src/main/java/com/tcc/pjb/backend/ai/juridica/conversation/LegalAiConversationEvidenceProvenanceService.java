package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAttachmentProvenanceClassifier;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidencePromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceSovereignRegistryService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceTrustClassifier;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationEvidenceProvenanceService {

    private final LegalEvidenceTrustClassifier evidenceTrustClassifier;
    private final LegalAttachmentProvenanceClassifier attachmentProvenanceClassifier;
    private final LegalEvidencePromotionPolicy evidencePromotionPolicy;
    private final LegalEvidenceSovereignRegistryService evidenceSovereignRegistryService;

    public LegalAiConversationEvidenceProvenanceService(LegalEvidenceTrustClassifier evidenceTrustClassifier,
                                                        LegalAttachmentProvenanceClassifier attachmentProvenanceClassifier,
                                                        LegalEvidencePromotionPolicy evidencePromotionPolicy,
                                                        LegalEvidenceSovereignRegistryService evidenceSovereignRegistryService) {
        this.evidenceTrustClassifier = Objects.requireNonNull(evidenceTrustClassifier, "evidenceTrustClassifier");
        this.attachmentProvenanceClassifier = Objects.requireNonNull(attachmentProvenanceClassifier, "attachmentProvenanceClassifier");
        this.evidencePromotionPolicy = Objects.requireNonNull(evidencePromotionPolicy, "evidencePromotionPolicy");
        this.evidenceSovereignRegistryService = Objects.requireNonNull(evidenceSovereignRegistryService, "evidenceSovereignRegistryService");
    }


    public LegalAiConversationEvidenceProvenanceSnapshot inspect(LegalAiConversationRequest request,
                                                                 String capability,
                                                                 String version,
                                                                 LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                 LegalAiConversationTrustZoneSnapshot trustZone,
                                                                 LegalAiConversationToolScopeSnapshot toolScope) {
        var sourceDecision = evidenceTrustClassifier.classify(request, documentSecurity, trustZone);
        var attachmentDecision = attachmentProvenanceClassifier.classify(request, documentSecurity, trustZone);
        var decision = evidencePromotionPolicy.resolve(request, capability, version, trustZone, toolScope, sourceDecision, attachmentDecision);
        var evidenceRegistry = evidenceSovereignRegistryService.materialize(request, documentSecurity, trustZone, sourceDecision, attachmentDecision, decision);
        return new LegalAiConversationEvidenceProvenanceSnapshot(
                decision.status(),
                decision.effectiveEvidenceTier(),
                decision.sourceEvidenceTier(),
                decision.attachmentEvidenceTier(),
                decision.sovereignProvenanceMode(),
                decision.ragPromotionStatus(),
                decision.groundingPromotionStatus(),
                decision.draftPromotionStatus(),
                decision.suggestionPromotionStatus(),
                decision.capabilityRecoveryPromotionStatus(),
                decision.officialEvidenceIds(),
                decision.institutionalControlledEvidenceIds(),
                decision.derivedEvidenceIds(),
                decision.untrustedEvidenceIds(),
                evidenceRegistry.descriptors(),
                evidenceRegistry.promotedRagEvidenceIds(),
                evidenceRegistry.promotedGroundingEvidenceIds(),
                evidenceRegistry.promotedDraftEvidenceIds(),
                evidenceRegistry.promotedSuggestionEvidenceIds(),
                evidenceRegistry.promotedCapabilityRecoveryEvidenceIds(),
                decision.blockedToolIds(),
                decision.elevatedStepUpToolIds(),
                decision.unmetRequirements(),
                decision.reasons(),
                mergeDiagnostics(decision.diagnostics(), evidenceRegistry.diagnostics())
        );
    }

    private java.util.Map<String, Object> mergeDiagnostics(java.util.Map<String, Object> first,
                                                           java.util.Map<String, Object> second) {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        if (first != null) {
            out.putAll(first);
        }
        if (second != null) {
            out.putAll(second);
        }
        return ImmutableViewSupport.map(out);
    }
}
