package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilitySuppressionSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationTrustZoneServiceTest {

    @Test
    void mustEscalateToCriticalZoneWhenSigiloAndQuarantineConverge() {
        var service = new LegalAiConversationTrustZoneService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-critical",
                        "PROC-CRIT-1",
                        "Quero minuta sigilosa com documentos controlados",
                        "ADVOGADO",
                        List.of(),
                        List.of("evidencia_prompt.pdf"),
                        Map.of("sigilo", "sigiloso")
                ),
                "LEGAL_RECURSAL_DRAFT_V3",
                "V3",
                new LegalAiConversationDocumentSecuritySnapshot(
                        "HUMAN_REVIEW_REQUIRED",
                        List.of("STJ"),
                        List.of("pastebin.com"),
                        List.of(),
                        List.of("evidencia_prompt.pdf"),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationToolScopeSnapshot(
                        "PROCESS_CLASS_SUPPRESSION_GATED",
                        List.of("LEGAL_RECURSAL_DRAFT", "LEGAL_CASE_WRITE"),
                        List.of(),
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "DEGRADED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilitySuppressionSnapshot(
                        "LOCKED",
                        true,
                        true,
                        "PROCESS_CLASS_SIGILO",
                        "PENAL",
                        "SIGILOSO",
                        "STRICT_SIGILO",
                        "CLASS_SIGILO_HARD_LOCK",
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        List.of("SIGILO_SENSITIVE_FLOW"),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("LOCKED", snapshot.status());
        assertEquals("CRITICAL", snapshot.trustZone());
        assertTrue(snapshot.sovereignBoundaryRequired());
        assertEquals("BLOCKED_EXTERNAL", snapshot.sourceZone());
        assertEquals("QUARANTINED", snapshot.attachmentZone());
        assertTrue(snapshot.blockedToolIds().contains("LEGAL_RECURSAL_DRAFT"));
    }

    @Test
    void mustKeepPublicZoneWhenContextIsPublicAndReadOnly() {
        var service = new LegalAiConversationTrustZoneService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-public",
                        null,
                        "Quero orientação pública sobre competência",
                        "CIDADAO",
                        List.of(),
                        List.of(),
                        Map.of("sigilo", "publico")
                ),
                "LEGAL_GUIDE_READ_V3",
                "V3",
                new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot("OPEN", List.of("LEGAL_GUIDE_READ"), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "READY", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilitySuppressionSnapshot(
                        "NOT_REQUIRED",
                        false,
                        false,
                        "SESSION_CLASS_SIGILO",
                        "CIVEL",
                        "PUBLICO",
                        "BASELINE",
                        "NONE",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("NOT_REQUIRED", snapshot.status());
        assertEquals("PUBLIC", snapshot.trustZone());
        assertFalse(snapshot.sovereignBoundaryRequired());
        assertEquals("PUBLIC_INTERNAL", snapshot.sourceZone());
        assertEquals("NONE", snapshot.attachmentZone());
    }
}
