package com.tcc.pjb.backend.service.intelligence.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationEvidenceProvenanceService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationTrustZoneService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAttachmentProvenanceClassifier;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDocumentQuarantineService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDraftPromotionFence;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidencePromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceSovereignRegistryService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceTrustClassifier;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalGroundingPromotionFence;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSourceAllowlist;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalToolScopePolicy;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiStructuredSurfaceGovernanceServiceTest {

    @Test
    void mustPromoteGroundingWhenOfficialChainIsStableOnStructuredSurface() {
        var service = service();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        null,
                        "PROC-10",
                        "Quero conferir citações oficiais do STJ",
                        "ADVOGADO",
                        List.of(),
                        List.of("acordao_assinado_oficial.pdf"),
                        Map.of(
                                "sourceSystem", "STJ",
                                "evidenceUrl", "https://www.stj.jus.br/processo/10",
                                "documentAuthority", "CNJ"
                        )
                ),
                "LEGAL_GROUNDING_CHECK_V3",
                "V3",
                List.of(
                        new LegalAiToolDescriptor("LEGAL_RAG_SEARCH", "RAG", "GROUNDING", true, true, true, false, "MCP_JURISPRUDENCIA"),
                        new LegalAiToolDescriptor("LEGAL_GROUNDING_CITATION", "Citation", "GROUNDING", true, true, true, false, "MCP_JURISPRUDENCIA")
                )
        );

        assertEquals("NOT_REQUIRED", snapshot.surfaceStatus());
        assertEquals("PROMOTED", snapshot.evidenceProvenance().groundingPromotionStatus());
        assertEquals("OFFICIAL_DOCUMENT", snapshot.evidenceProvenance().effectiveEvidenceTier());
        assertTrue(snapshot.safeguards().containsKey("groundingPromotionStatus"));
        assertTrue(snapshot.safeguards().get("promotedGroundingEvidenceIds").toString().contains("STJ"));
        assertTrue(snapshot.nextSteps().stream().anyMatch(item -> item.contains("fonte oficial") || item.contains("oficial")));
    }

    @Test
    void mustBlockDraftWhenDerivedAndExternalEvidenceTouchesMutatingSurface() {
        var service = service();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        null,
                        "PROC-11",
                        "Faça uma minuta com base nessas notas resumidas",
                        "ADVOGADO",
                        List.of(),
                        List.of("resumo_ocr_quarentena.pdf"),
                        Map.of(
                                "documentSource", "workspace_institucional",
                                "notesDocument", "sumario derivado interno",
                                "evidenceUrl", "https://pastebin.com/raw/unsafe"
                        )
                ),
                "LEGAL_DRAFT_V2",
                "V2",
                List.of(
                        new LegalAiToolDescriptor("LEGAL_DRAFT_PETITION", "Draft", "PETICIONAMENTO", false, false, true, true, "PJB_PROTOCOL"),
                        new LegalAiToolDescriptor("LEGAL_RAG_SEARCH", "RAG", "GROUNDING", true, true, true, false, "MCP_JURISPRUDENCIA")
                )
        );

        assertEquals("LOCKED", snapshot.surfaceStatus());
        assertEquals("BLOCKED", snapshot.evidenceProvenance().draftPromotionStatus());
        assertEquals("UNTRUSTED_DOCUMENT", snapshot.evidenceProvenance().effectiveEvidenceTier());
        assertTrue(snapshot.safeguards().get("blockedToolIds").toString().contains("LEGAL_DRAFT_PETITION"));
        assertTrue(snapshot.safeguards().get("evidenceDescriptors").toString().contains("resumo_ocr_quarentena.pdf"));
        assertTrue(snapshot.nextSteps().stream().anyMatch(item -> item.contains("quarentena") || item.contains("Proveniência pendente")));
    }

    private LegalAiStructuredSurfaceGovernanceService service() {
        return new LegalAiStructuredSurfaceGovernanceService(
                new LegalContextSanitizer(),
                new LegalSourceAllowlist(),
                new LegalDocumentQuarantineService(),
                new LegalToolScopePolicy(),
                new LegalAiConversationTrustZoneService(),
                new LegalAiConversationEvidenceProvenanceService(
                        new LegalEvidenceTrustClassifier(),
                        new LegalAttachmentProvenanceClassifier(),
                        new LegalEvidencePromotionPolicy(new LegalGroundingPromotionFence(), new LegalDraftPromotionFence()),
                        new LegalEvidenceSovereignRegistryService()
                )
        );
    }
}
