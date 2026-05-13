package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecurrenceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationCapabilitySuppressionServiceTest {

    @Test
    void mustLockStrictClassWhenSigiloAndRecurrenceAreBothCritical() {
        var service = new LegalAiConversationCapabilitySuppressionService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-penal",
                        "PROC-PENAL-1",
                        "Quero insistir na minuta do recurso criminal sigiloso",
                        "ADVOGADO",
                        List.of(),
                        List.of(),
                        Map.of("ramo", "penal", "classe", "apelação criminal", "sigilo", "sigiloso")
                ),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationDocumentSecuritySnapshot("HUMAN_REVIEW_REQUIRED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot(
                        "PROCESS_RECURRENCE_ESCALATED",
                        List.of("LEGAL_RECURSAL_DRAFT", "LEGAL_CASE_WRITE"),
                        List.of(),
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of(),
                        Map.of()
                ),
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "DEGRADED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRecurrenceSnapshot(
                        "LOCKED",
                        true,
                        true,
                        "conv-penal|PROC-PENAL-1|LEGAL_RECURSAL_ASSIST_V3",
                        4,
                        3,
                        true,
                        1,
                        "CRITICAL",
                        "PROCESS_SCOPED_HARD_LOCK",
                        List.of("LEGAL_RECURSAL_DRAFT"),
                        List.of("CAPABILITY_RECURRENCE_ESCALATED"),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("LOCKED", snapshot.status());
        assertTrue(snapshot.suppressionDetected());
        assertEquals("PENAL", snapshot.processClass());
        assertEquals("STRICT_SIGILO", snapshot.policyTier());
        assertEquals("CLASS_SIGILO_HARD_LOCK", snapshot.suppressionMode());
        assertTrue(snapshot.blockedToolIds().contains("LEGAL_RECURSAL_DRAFT"));
        assertTrue(snapshot.unmetRequirements().contains("SIGILO_SENSITIVE_FLOW"));
    }

    @Test
    void mustOnlyMonitorWhenClassIsBaselineAndNoSensitiveSignalExists() {
        var service = new LegalAiConversationCapabilitySuppressionService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-civel",
                        "PROC-CIVEL-1",
                        "Quero revisar a tese cível",
                        "ADVOGADO",
                        List.of(),
                        List.of(),
                        Map.of("ramo", "civel", "classe", "apelação cível", "sigilo", "publico")
                ),
                "LEGAL_PROCEDURAL_GUIDE_V3",
                "V3",
                new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot("OPEN", List.of("LEGAL_GUIDE_READ"), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionDoctorSnapshot("READY", false, false, "SESSION_READY", List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationSessionBootstrapSnapshot("READY", false, false, "SESSION_READY", "READY", "READY", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationCapabilityRecurrenceSnapshot(
                        "NOT_REQUIRED",
                        false,
                        true,
                        "conv-civel|PROC-CIVEL-1|LEGAL_PROCEDURAL_GUIDE_V3",
                        0,
                        0,
                        false,
                        0,
                        "LOW",
                        "NONE",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("NOT_REQUIRED", snapshot.status());
        assertTrue(!snapshot.suppressionDetected());
        assertEquals("CIVEL", snapshot.processClass());
        assertEquals("BASELINE", snapshot.policyTier());
    }
}
