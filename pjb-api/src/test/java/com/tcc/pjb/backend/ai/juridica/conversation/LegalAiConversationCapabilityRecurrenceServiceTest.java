package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecoverySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRehabilitationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationCapabilityRecurrenceServiceTest {

    @Test
    void mustLockCapabilityWhenSameProcessShowsRepeatedRecidivism() {
        var service = new LegalAiConversationCapabilityRecurrenceService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-123",
                        "PROC-123",
                        "Quero insistir novamente na minuta recursal desta mesma causa",
                        "ADVOGADO",
                        List.of("h1", "h2", "h3", "h4"),
                        List.of(),
                        Map.of("sigilo", "RESTRITO")
                ),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-123",
                        "PROC-123",
                        "ADVOGADO",
                        List.of(
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "processoId", "PROC-123", "approvalStatus", "STEP_UP_REQUIRED", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of("fundamento")),
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "processoId", "PROC-123", "approvalStatus", "HUMAN_REVIEW_REQUIRED", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of("choque"), "missingEvidence", List.of()),
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "processoId", "PROC-123", "approvalStatus", "READONLY_RESTRICTED", "hallucinationStatus", "BLOCKED", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of()),
                                Map.of("capability", "LEGAL_RECURSAL_ASSIST_V3", "processoId", "PROC-123", "approvalStatus", "AUTO_READONLY", "hallucinationStatus", "READY", "symbolicExecutionStatus", "BLOCKED", "contradictions", List.of(), "missingEvidence", List.of())
                        ),
                        Map.of(),
                        Map.of("retainedTurnCount", 4)
                ),
                new LegalAiConversationDocumentSecuritySnapshot("HUMAN_REVIEW_REQUIRED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot(
                        "CAPABILITY_REHABILITATION_PENDING",
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationSessionDoctorSnapshot("DEGRADED", false, true, "SESSION_MONITORED", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("DEGRADED", false, true, "SESSION_BOOTSTRAP_GATED", "READY", "DEGRADED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRecoverySnapshot("RECOVERED", true, true, "RECOVERY_STEP_UP_MONITORED", List.of("LEGAL_RECURSAL_DRAFT"), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityCooldownSnapshot("LOCKED", true, "SESSION_PROCESS", "conv-123|PROC-123|LEGAL_RECURSAL_ASSIST_V3", 2, true, List.of("LEGAL_RECURSAL_DRAFT"), List.of(), Map.of()),
                new LegalAiConversationCapabilityRehabilitationSnapshot("BLOCKED", true, false, false, "REHABILITATION_DENIED", 0, 3, 3, List.of(), List.of("LEGAL_RECURSAL_DRAFT"), List.of(), List.of(), Map.of())
        );

        assertEquals("LOCKED", snapshot.status());
        assertTrue(snapshot.recurrenceDetected());
        assertTrue(snapshot.processScoped());
        assertEquals("CRITICAL", snapshot.riskTier());
        assertEquals("PROCESS_SCOPED_HARD_LOCK", snapshot.escalationMode());
        assertTrue(snapshot.recurrenceCount() >= 4);
        assertTrue(snapshot.failedRehabilitationCount() >= 3);
        assertTrue(snapshot.blockedToolIds().contains("LEGAL_RECURSAL_DRAFT"));
        assertTrue(snapshot.unmetRequirements().contains("PROCESS_RECIDIVISM_THRESHOLD_REACHED"));
    }

    @Test
    void mustRemainNotRequiredWhenSessionHasNoRecurrenceSignal() {
        var service = new LegalAiConversationCapabilityRecurrenceService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-123-ok",
                        "PROC-123-ok",
                        "Quero só revisar a orientação processual",
                        "ADVOGADO",
                        List.of(),
                        List.of(),
                        Map.of("sigilo", "PUBLICO")
                ),
                "LEGAL_PROCEDURAL_GUIDE_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-123-ok",
                        "PROC-123-ok",
                        "ADVOGADO",
                        List.of(
                                Map.of("capability", "LEGAL_PROCEDURAL_GUIDE_V3", "processoId", "PROC-123-ok", "approvalStatus", "AUTO_READONLY", "hallucinationStatus", "READY", "symbolicExecutionStatus", "READY", "contradictions", List.of(), "missingEvidence", List.of())
                        ),
                        Map.of(),
                        Map.of("retainedTurnCount", 1)
                ),
                new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot("OPEN", List.of("LEGAL_GUIDE_READ"), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "READY", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRecoverySnapshot("NOT_REQUIRED", false, false, "NONE", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityCooldownSnapshot("NOT_REQUIRED", false, "SESSION_PROCESS", "conv-123-ok|PROC-123-ok|LEGAL_PROCEDURAL_GUIDE_V3", 0, false, List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRehabilitationSnapshot("NOT_REQUIRED", false, false, false, "NONE", 0, 3, 0, List.of(), List.of(), List.of(), List.of(), Map.of())
        );

        assertEquals("NOT_REQUIRED", snapshot.status());
        assertTrue(!snapshot.recurrenceDetected());
        assertEquals("LOW", snapshot.riskTier());
        assertEquals("NONE", snapshot.escalationMode());
    }
}
