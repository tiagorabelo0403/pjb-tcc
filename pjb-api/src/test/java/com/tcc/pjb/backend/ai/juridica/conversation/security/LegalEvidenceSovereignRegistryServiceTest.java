package com.tcc.pjb.backend.ai.juridica.conversation.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAttachmentProvenanceClassifier.AttachmentProvenanceDecision;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidencePromotionPolicy.EvidencePromotionDecision;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceTrustClassifier.EvidenceTrustDecision;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalEvidenceSovereignRegistryServiceTest {

    @Test
    void mustMaterializePromotedOfficialEvidenceDescriptors() {
        var service = new LegalEvidenceSovereignRegistryService();
        var registry = service.materialize(
                new LegalAiConversationRequest(
                        "conv-128",
                        "PROC-128",
                        "Quero conferir o lastro oficial",
                        "ADVOGADO",
                        List.of(),
                        List.of("acordao_assinado_oficial.pdf"),
                        Map.of(
                                "sourceSystem", "STJ",
                                "documentAuthority", "CNJ"
                        )
                ),
                new LegalAiConversationDocumentSecuritySnapshot(
                        "CLEARED",
                        List.of("STJ", "CNJ"),
                        List.of(),
                        List.of("acordao_assinado_oficial.pdf"),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationTrustZoneSnapshot(
                        "NOT_REQUIRED",
                        "PUBLIC",
                        false,
                        true,
                        "PUBLIC_INTERNAL",
                        "ALLOWLISTED",
                        "READ_ONLY",
                        "PUBLIC_READ_ONLY",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new EvidenceTrustDecision(
                        "OFFICIAL_DOCUMENT",
                        List.of("STJ", "CNJ"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new AttachmentProvenanceDecision(
                        "OFFICIAL_DOCUMENT",
                        List.of("acordao_assinado_oficial.pdf"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new EvidencePromotionDecision(
                        "NOT_REQUIRED",
                        "OFFICIAL_DOCUMENT",
                        "OFFICIAL_DOCUMENT",
                        "OFFICIAL_DOCUMENT",
                        "OFFICIAL_CHAIN",
                        "PROMOTED",
                        "PROMOTED",
                        "PROMOTED",
                        "PROMOTED",
                        "PROMOTED",
                        List.of("STJ", "CNJ", "acordao_assinado_oficial.pdf"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals(3, registry.descriptors().size());
        assertTrue(registry.promotedGroundingEvidenceIds().contains("STJ"));
        assertTrue(registry.promotedDraftEvidenceIds().contains("acordao_assinado_oficial.pdf"));
        assertTrue(registry.descriptors().stream().anyMatch(item -> "SIGNED".equals(item.signatureStatus()) && "acordao_assinado_oficial.pdf".equals(item.evidenceId())));
        assertTrue(registry.descriptors().stream().allMatch(item -> item.integrityHash() != null && item.integrityHash().length() == 64));
    }
}
