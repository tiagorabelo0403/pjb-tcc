package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecoverySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationCapabilityCooldownServiceTest {

    @Test
    void mustLockRecoveredCapabilityWhenRecentTurnsShowFlapping() {
        var service = new LegalAiConversationCapabilityCooldownService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-121",
                        "PROC-121",
                        "Preciso insistir na minuta recursal agora",
                        "ADVOGADO",
                        List.of("t1", "t2", "t3"),
                        List.of(),
                        Map.of("sigilo", "RESTRITO")
                ),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-121",
                        "PROC-121",
                        "ADVOGADO",
                        List.of(
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "approvalStatus", "STEP_UP_REQUIRED", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of("falta fundamento")),
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "approvalStatus", "HUMAN_REVIEW_REQUIRED", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of("contradição"), "missingEvidence", List.of()),
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "approvalStatus", "AUTO_READONLY", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of())
                        ),
                        Map.of(),
                        Map.of("retainedTurnCount", 3)
                ),
                new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot(
                        "SESSION_RECOVERY_GATED",
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationSessionDoctorSnapshot(
                        "READY",
                        false,
                        true,
                        "SESSION_MONITORED",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationSessionBootstrapSnapshot(
                        "DEGRADED",
                        false,
                        true,
                        "SESSION_BOOTSTRAP_GATED",
                        "READY",
                        "DEGRADED",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationCapabilityRecoverySnapshot(
                        "RECOVERED",
                        true,
                        true,
                        "RECOVERY_STEP_UP_MONITORED",
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("LOCKED", snapshot.status());
        assertTrue(snapshot.lockActive());
        assertTrue(snapshot.blockedCapability());
        assertEquals("SESSION_PROCESS", snapshot.lockScope());
        assertTrue(snapshot.cooldownTurnsRemaining() >= 1);
        assertTrue(snapshot.blockedToolIds().contains("LEGAL_RECURSAL_DRAFT"));
    }

    @Test
    void mustStayNotRequiredWhenNoRecoveryOrBootstrapRiskExists() {
        var service = new LegalAiConversationCapabilityCooldownService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-121-b",
                        null,
                        "Só quero pesquisa jurisprudencial",
                        "ADVOGADO",
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                "LEGAL_RESEARCH_V1",
                "V1",
                new LegalAiConversationMemorySnapshot(
                        "conv-121-b",
                        null,
                        "ADVOGADO",
                        List.of(),
                        Map.of(),
                        Map.of()
                ),
                new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot("OPEN", List.of("LEGAL_SEARCH"), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "READY", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRecoverySnapshot("NOT_REQUIRED", false, false, "NONE", List.of(), List.of(), List.of(), Map.of())
        );

        assertEquals("NOT_REQUIRED", snapshot.status());
        assertFalse(snapshot.lockActive());
        assertEquals(0, snapshot.cooldownTurnsRemaining());
    }
}
