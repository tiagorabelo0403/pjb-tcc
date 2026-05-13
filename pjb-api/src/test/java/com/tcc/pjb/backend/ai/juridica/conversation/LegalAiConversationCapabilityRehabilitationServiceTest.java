package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot;
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

class LegalAiConversationCapabilityRehabilitationServiceTest {

    @Test
    void mustReleaseCapabilityAfterWinningStabilityWindow() {
        var service = new LegalAiConversationCapabilityRehabilitationService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-122",
                        "PROC-122",
                        "Quero retomar a minuta recursal com segurança",
                        "ADVOGADO",
                        List.of("h1", "h2", "h3"),
                        List.of(),
                        Map.of("sigilo", "PUBLICO")
                ),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-122",
                        "PROC-122",
                        "ADVOGADO",
                        List.of(
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "approvalStatus", "AUTO_READONLY", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of()),
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "approvalStatus", "AUTO_READONLY", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of()),
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
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "READY", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRecoverySnapshot(
                        "RECOVERED",
                        true,
                        true,
                        "RECOVERY_STEP_UP_MONITORED",
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationCapabilityCooldownSnapshot(
                        "MONITORED",
                        false,
                        "SESSION_PROCESS",
                        "conv-122|PROC-122|LEGAL_RECURSAL_ASSIST_V3",
                        0,
                        false,
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("RELEASED", snapshot.status());
        assertTrue(snapshot.releaseEligible());
        assertTrue(snapshot.capabilityReleased());
        assertEquals("REHABILITATION_STEP_UP_GATED", snapshot.releaseLane());
        assertEquals(3, snapshot.stableWinningTurns());
        assertEquals(0, snapshot.rehabilitationWindowTurnsRemaining());
        assertTrue(snapshot.releasedToolIds().contains("LEGAL_RECURSAL_DRAFT"));
    }

    @Test
    void mustKeepCapabilityInCountingWindowWhenStableTurnsAreInsufficient() {
        var service = new LegalAiConversationCapabilityRehabilitationService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-122-b",
                        "PROC-122-b",
                        "Ainda quero retomar a capability recursal",
                        "ADVOGADO",
                        List.of("h1"),
                        List.of(),
                        Map.of("sigilo", "PUBLICO")
                ),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-122-b",
                        "PROC-122-b",
                        "ADVOGADO",
                        List.of(
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "approvalStatus", "AUTO_READONLY", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of())
                        ),
                        Map.of(),
                        Map.of("retainedTurnCount", 1)
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
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "READY", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRecoverySnapshot(
                        "RECOVERED",
                        true,
                        true,
                        "RECOVERY_STEP_UP_MONITORED",
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationCapabilityCooldownSnapshot(
                        "MONITORED",
                        false,
                        "SESSION_PROCESS",
                        "conv-122-b|PROC-122-b|LEGAL_RECURSAL_ASSIST_V3",
                        0,
                        false,
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("MONITORED", snapshot.status());
        assertTrue(snapshot.rehabilitationRequired());
        assertTrue(snapshot.releaseEligible());
        assertTrue(!snapshot.capabilityReleased());
        assertEquals(1, snapshot.stableWinningTurns());
        assertEquals(2, snapshot.rehabilitationWindowTurnsRemaining());
        assertTrue(snapshot.unmetRequirements().contains("STABILITY_WINDOW_INCOMPLETE"));
    }
}
