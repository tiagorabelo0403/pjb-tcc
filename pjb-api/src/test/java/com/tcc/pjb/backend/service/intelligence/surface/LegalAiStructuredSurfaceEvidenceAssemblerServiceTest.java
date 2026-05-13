package com.tcc.pjb.backend.service.intelligence.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeCommentaryTextCatalogService;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.surface.LegalAiStructuredSurfaceGovernanceSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiStructuredSurfaceEvidenceAssemblerServiceTest {

    private final LegalAiStructuredSurfaceEvidenceAssemblerService service = new LegalAiStructuredSurfaceEvidenceAssemblerService(
            new LegalKnowledgeCommentaryTextCatalogService(new LegalKnowledgeJsonResourceLoader(new ObjectMapper()))
    );

    @Test
    void mustExposeOnlyPromotedDraftEvidenceOnMutatingSurface() {
        var governance = governance(
                new LegalAiConversationEvidenceDescriptor(
                        "CNJ-OFICIAL-001",
                        "SOURCE",
                        "OFFICIAL_DOCUMENT",
                        "SYSTEM_SOURCE",
                        "CNJ",
                        "CNJ-OFICIAL-001",
                        "hash-1",
                        "SYSTEM_VERIFIED",
                        "DIRECT_OFFICIAL",
                        false,
                        List.of(),
                        List.of(),
                        true,
                        true,
                        true,
                        true,
                        true
                ),
                new LegalAiConversationEvidenceDescriptor(
                        "OCR-RESUMO-001",
                        "ATTACHMENT",
                        "DERIVED_DOCUMENT",
                        "ATTACHMENT_UPLOAD",
                        "PJB",
                        "OCR-RESUMO-001",
                        "hash-2",
                        "UNKNOWN",
                        "OCR_DERIVED",
                        false,
                        List.of(),
                        List.of("DERIVED_CHAIN_REQUIRES_CONFIRMATION"),
                        false,
                        false,
                        false,
                        false,
                        false
                )
        );

        var bundle = service.assembleForDraft(governance);

        assertEquals("LEGAL_DRAFT_V2", bundle.surfaceCode());
        assertEquals("PROMOTED", bundle.promotionStatus());
        assertTrue(bundle.anchored());
        assertEquals(List.of("CNJ-OFICIAL-001"), bundle.promotedEvidenceIds());
        assertEquals(1, bundle.promotedEvidenceDescriptors().size());
        assertEquals("CNJ-OFICIAL-001", bundle.promotedEvidenceDescriptors().getFirst().evidenceId());
        assertFalse(bundle.reasons().contains("SURFACE_PROMOTION_NOT_ANCHORED"));
    }

    @Test
    void mustRemainUnanchoredWhenGroundingStepUpHasNoPromotedEvidenceDescriptor() {
        var evidenceProvenance = new LegalAiConversationEvidenceProvenanceSnapshot(
                "ESCALATED",
                "DERIVED_DOCUMENT",
                "DERIVED_DOCUMENT",
                "NO_EVIDENCE",
                "SOVEREIGN_STEP_UP",
                "STEP_UP_REQUIRED",
                "STEP_UP_REQUIRED",
                "STEP_UP_REQUIRED",
                "MONITORED",
                "STEP_UP_REQUIRED",
                List.of(),
                List.of(),
                List.of("OCR-RESUMO-002"),
                List.of(),
                List.of(
                        new LegalAiConversationEvidenceDescriptor(
                                "OCR-RESUMO-002",
                                "ATTACHMENT",
                                "DERIVED_DOCUMENT",
                                "ATTACHMENT_UPLOAD",
                                "PJB",
                                "OCR-RESUMO-002",
                                "hash-3",
                                "UNKNOWN",
                                "OCR_DERIVED",
                                false,
                                List.of(),
                                List.of("DERIVED_CHAIN_REQUIRES_CONFIRMATION"),
                                false,
                                false,
                                false,
                                false,
                                false
                        )
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("LEGAL_GROUNDING_CITATION"),
                List.of(),
                List.of("DERIVED_EVIDENCE_REQUIRES_SOVEREIGN_CONFIRMATION"),
                List.of("Grounding promotion=STEP_UP_REQUIRED para tier efetivo DERIVED_DOCUMENT."),
                Map.of("groundingPromotionStatus", "STEP_UP_REQUIRED")
        );

        var bundle = service.assembleForGrounding(new LegalAiStructuredSurfaceGovernanceSnapshot(null, null, null, null, null, evidenceProvenance));

        assertEquals("LEGAL_GROUNDING_CHECK_V3", bundle.surfaceCode());
        assertEquals("STEP_UP_REQUIRED", bundle.promotionStatus());
        assertFalse(bundle.anchored());
        assertTrue(bundle.promotedEvidenceIds().isEmpty());
        assertTrue(bundle.reasons().contains("SURFACE_PROMOTION_NOT_ANCHORED"));
        assertTrue(bundle.unmetRequirements().contains("DERIVED_EVIDENCE_REQUIRES_SOVEREIGN_CONFIRMATION"));
    }

    private LegalAiStructuredSurfaceGovernanceSnapshot governance(LegalAiConversationEvidenceDescriptor... descriptors) {
        var evidenceProvenance = new LegalAiConversationEvidenceProvenanceSnapshot(
                "NOT_REQUIRED",
                "OFFICIAL_DOCUMENT",
                "OFFICIAL_DOCUMENT",
                "NO_EVIDENCE",
                "SOVEREIGN_PROMOTED",
                "PROMOTED",
                "PROMOTED",
                "PROMOTED",
                "PROMOTED",
                "PROMOTED",
                List.of("CNJ-OFICIAL-001"),
                List.of(),
                List.of("OCR-RESUMO-001"),
                List.of(),
                List.of(descriptors),
                List.of("CNJ-OFICIAL-001"),
                List.of("CNJ-OFICIAL-001"),
                List.of("CNJ-OFICIAL-001"),
                List.of("CNJ-OFICIAL-001"),
                List.of("CNJ-OFICIAL-001"),
                List.of(),
                List.of(),
                List.of(),
                List.of("Draft promotion=PROMOTED para tier efetivo OFFICIAL_DOCUMENT."),
                Map.of("draftPromotionStatus", "PROMOTED")
        );
        return new LegalAiStructuredSurfaceGovernanceSnapshot(null, null, null, null, null, evidenceProvenance);
    }
}
