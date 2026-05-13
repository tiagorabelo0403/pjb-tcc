package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationCapabilityRecoveryServiceTest {

    @Test
    void mustRecoverBlockedCapabilityWhenReplayDoctorAndCoverageConverge() {
        var service = new LegalAiConversationCapabilityRecoveryService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-120",
                        "PROC-120",
                        "Preciso revisar minuta recursal com segurança",
                        "ADVOGADO",
                        List.of("turno 1", "turno 2", "turno 3"),
                        List.of(),
                        Map.of("sigilo", "RESTRITO")
                ),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-120",
                        "PROC-120",
                        "ADVOGADO",
                        List.of(Map.of(), Map.of(), Map.of()),
                        Map.of(),
                        Map.of("retainedTurnCount", 3)
                ),
                new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot(
                        "SESSION_BOOTSTRAP_BLOCKED",
                        List.of(),
                        List.of("LEGAL_RECURSAL_SEARCH", "LEGAL_RECURSAL_DRAFT"),
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of("bootstrap blocked"),
                        Map.of(
                                "mcpTranscriptReplayReady", true,
                                "mcpBenchmarkPassed", true,
                                "mcpDoctorReady", true,
                                "mcpDoctorStatus", "READY",
                                "mcpEvidencePromotionStatus", "PROMOTED_FROM_REPLAY",
                                "sessionBootstrapRecoveryCandidateToolIds", List.of("LEGAL_RECURSAL_DRAFT")
                        )
                ),
                new LegalAiConversationSessionDoctorSnapshot(
                        "READY",
                        false,
                        false,
                        "SESSION_READY",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationSessionBootstrapSnapshot(
                        "BLOCKED",
                        true,
                        false,
                        "SESSION_BOOTSTRAP_LOCKDOWN",
                        "READY",
                        "DEGRADED",
                        List.of("LEGAL_SKILL_RECURSAL"),
                        List.of("EXAMPLE_RECURSO"),
                        List.of(),
                        List.of(),
                        List.of("bootstrap blocked for replay gating"),
                        Map.of()
                )
        );

        assertEquals("RECOVERED", snapshot.status());
        assertTrue(snapshot.recoveryEligible());
        assertTrue(snapshot.capabilityRecovered());
        assertEquals("RECOVERY_STEP_UP_MONITORED", snapshot.recoveryLane());
        assertEquals(List.of("LEGAL_RECURSAL_DRAFT"), snapshot.recoveryCandidateToolIds());
        assertTrue(snapshot.unmetRequirements().isEmpty());
    }

    @Test
    void mustDenyRecoveryWhenProfileOrSigiloStayBlocked() {
        var service = new LegalAiConversationCapabilityRecoveryService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-120-b",
                        "PROC-120-B",
                        "Quero minuta decisória sigilosa",
                        "CIDADAO",
                        List.of("turno 1"),
                        List.of(),
                        Map.of("sigilo", "SIGILOSO")
                ),
                "LEGAL_DECISAO_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-120-b",
                        "PROC-120-B",
                        "CIDADAO",
                        List.of(Map.of()),
                        Map.of(),
                        Map.of("retainedTurnCount", 1)
                ),
                new LegalAiConversationDocumentSecuritySnapshot("HUMAN_REVIEW_REQUIRED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot(
                        "SESSION_BOOTSTRAP_BLOCKED",
                        List.of(),
                        List.of("LEGAL_DECISAO_DRAFT"),
                        List.of(),
                        List.of(),
                        Map.of(
                                "mcpTranscriptReplayReady", true,
                                "mcpBenchmarkPassed", true,
                                "mcpDoctorReady", true,
                                "mcpDoctorStatus", "READY",
                                "mcpEvidencePromotionStatus", "PROMOTED_FROM_REPLAY"
                        )
                ),
                new LegalAiConversationSessionDoctorSnapshot(
                        "READY",
                        false,
                        false,
                        "SESSION_READY",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationSessionBootstrapSnapshot(
                        "BLOCKED",
                        true,
                        true,
                        "SESSION_BOOTSTRAP_LOCKDOWN",
                        "BLOCKED",
                        "BLOCKED",
                        List.of("LEGAL_SKILL_DECISORY_DRAFT"),
                        List.of("EXAMPLE_DECISORIO"),
                        List.of("LEGAL_SKILL_DECISORY_DRAFT"),
                        List.of("EXAMPLE_DECISORIO"),
                        List.of("profile blocked"),
                        Map.of()
                )
        );

        assertEquals("DENIED", snapshot.status());
        assertFalse(snapshot.recoveryEligible());
        assertFalse(snapshot.capabilityRecovered());
        assertTrue(snapshot.unmetRequirements().contains("PROFILE_GATE_BLOCKED"));
        assertTrue(snapshot.unmetRequirements().contains("SIGILO_FENCE_BLOCKED"));
    }
}
