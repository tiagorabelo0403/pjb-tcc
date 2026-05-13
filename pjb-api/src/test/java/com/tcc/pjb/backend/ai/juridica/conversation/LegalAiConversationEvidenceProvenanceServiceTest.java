package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAttachmentProvenanceClassifier;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDraftPromotionFence;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidencePromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceSovereignRegistryService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceTrustClassifier;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalGroundingPromotionFence;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationEvidenceProvenanceServiceTest {

    @Test
    void mustPromoteOfficialEvidenceForGroundingWhenChainIsSovereignAndStable() {
        var service = new LegalAiConversationEvidenceProvenanceService(
                new LegalEvidenceTrustClassifier(),
                new LegalAttachmentProvenanceClassifier(),
                new LegalEvidencePromotionPolicy(new LegalGroundingPromotionFence(), new LegalDraftPromotionFence()),
                new LegalEvidenceSovereignRegistryService()
        );

        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-official",
                        "PROC-1",
                        "Quero fundamentação com precedentes oficiais",
                        "ADVOGADO",
                        List.of(),
                        List.of("acordao_oficial.pdf"),
                        Map.of(
                                "sourceSystem", "STJ",
                                "evidenceUrl", "https://www.stj.jus.br/processo/123",
                                "documentAuthority", "CNJ"
                        )
                ),
                "LEGAL_GROUNDING_V3",
                "V3",
                new LegalAiConversationDocumentSecuritySnapshot(
                        "CLEARED",
                        List.of("STJ", "CNJ"),
                        List.of(),
                        List.of("acordao_oficial.pdf"),
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
                new LegalAiConversationToolScopeSnapshot(
                        "OPEN",
                        List.of("LEGAL_RAG_SEARCH", "LEGAL_GROUNDING_CITATION"),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of("capabilityRecoveryCandidateToolIds", List.of("LEGAL_GROUNDING_CITATION"))
                )
        );

        assertEquals("NOT_REQUIRED", snapshot.status());
        assertEquals("OFFICIAL_DOCUMENT", snapshot.effectiveEvidenceTier());
        assertEquals("PROMOTED", snapshot.ragPromotionStatus());
        assertEquals("PROMOTED", snapshot.groundingPromotionStatus());
        assertEquals("NOT_REQUIRED", snapshot.draftPromotionStatus());
        assertEquals("PROMOTED", snapshot.capabilityRecoveryPromotionStatus());
        assertTrue(snapshot.officialEvidenceIds().contains("STJ"));
        assertTrue(snapshot.officialEvidenceIds().contains("acordao_oficial.pdf"));
        assertTrue(snapshot.promotedGroundingEvidenceIds().contains("STJ"));
        assertTrue(snapshot.evidenceDescriptors().stream().anyMatch(item -> "acordao_oficial.pdf".equals(item.evidenceId()) && item.promotedForGrounding()));
    }

    @Test
    void mustLockPromotionWhenDerivedAndQuarantinedEvidenceTouchesMutatingFlow() {
        var service = new LegalAiConversationEvidenceProvenanceService(
                new LegalEvidenceTrustClassifier(),
                new LegalAttachmentProvenanceClassifier(),
                new LegalEvidencePromotionPolicy(new LegalGroundingPromotionFence(), new LegalDraftPromotionFence()),
                new LegalEvidenceSovereignRegistryService()
        );

        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-derived",
                        "PROC-2",
                        "Faça uma minuta de petição com base nessas notas resumidas",
                        "ADVOGADO",
                        List.of(),
                        List.of("resumo_ocr_quarentena.pdf"),
                        Map.of(
                                "documentSource", "workspace_institucional",
                                "notesDocument", "sumario derivado interno",
                                "evidenceUrl", "https://pastebin.com/raw/abc"
                        )
                ),
                "LEGAL_DRAFT_PETITION_V3",
                "V3",
                new LegalAiConversationDocumentSecuritySnapshot(
                        "HUMAN_REVIEW_REQUIRED",
                        List.of(),
                        List.of("pastebin.com"),
                        List.of(),
                        List.of("resumo_ocr_quarentena.pdf"),
                        List.of("QUARANTINE"),
                        Map.of()
                ),
                new LegalAiConversationTrustZoneSnapshot(
                        "LOCKED",
                        "CRITICAL",
                        true,
                        true,
                        "BLOCKED_EXTERNAL",
                        "QUARANTINED",
                        "MUTATING",
                        "SOVEREIGN_HARD_LOCK",
                        List.of("LEGAL_DRAFT_PETITION"),
                        List.of(),
                        List.of("QUARANTINED_ATTACHMENT"),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationToolScopeSnapshot(
                        "TRUST_ZONE_LOCKED",
                        List.of("LEGAL_RAG_SEARCH", "LEGAL_DRAFT_PETITION", "LEGAL_CAPABILITY_RECOVERY"),
                        List.of("LEGAL_DRAFT_PETITION"),
                        List.of("LEGAL_RAG_SEARCH"),
                        List.of(),
                        Map.of("capabilityRecoveryCandidateToolIds", List.of("LEGAL_CAPABILITY_RECOVERY"))
                )
        );

        assertEquals("LOCKED", snapshot.status());
        assertEquals("UNTRUSTED_DOCUMENT", snapshot.effectiveEvidenceTier());
        assertEquals("BLOCKED", snapshot.ragPromotionStatus());
        assertEquals("BLOCKED", snapshot.groundingPromotionStatus());
        assertEquals("BLOCKED", snapshot.draftPromotionStatus());
        assertEquals("BLOCKED", snapshot.capabilityRecoveryPromotionStatus());
        assertTrue(snapshot.untrustedEvidenceIds().contains("pastebin.com"));
        assertTrue(snapshot.untrustedEvidenceIds().contains("resumo_ocr_quarentena.pdf"));
        assertTrue(snapshot.evidenceDescriptors().stream().anyMatch(item -> "resumo_ocr_quarentena.pdf".equals(item.evidenceId()) && item.quarantined()));
        assertTrue(snapshot.blockedToolIds().contains("LEGAL_DRAFT_PETITION"));
        assertTrue(snapshot.blockedToolIds().contains("LEGAL_CAPABILITY_RECOVERY"));
    }
}
