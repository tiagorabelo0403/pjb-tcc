package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationSessionDoctorServiceTest {

    @Test
    void mustBlockSurfaceWhenReplayDriftsAcrossRetainedTurns() {
        var service = new LegalAiConversationSessionDoctorService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest("conv-118", "PROC-88", "Quero fundamentos recursais", "ADVOGADO", List.of(), List.of(), Map.of()),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-118",
                        "PROC-88",
                        "ADVOGADO",
                        List.of(
                                Map.of("approvalStatus", "AUTO_READONLY", "hallucinationStatus", "ALLOW", "symbolicExecutionStatus", "PASS", "contradictions", List.of()),
                                Map.of("approvalStatus", "STEP_UP_REQUIRED", "hallucinationStatus", "BLOCKED", "symbolicExecutionStatus", "WARN", "missingEvidence", List.of("precedente não confirmado")),
                                Map.of("approvalStatus", "HUMAN_REVIEW_REQUIRED", "hallucinationStatus", "ALLOW", "symbolicExecutionStatus", "PASS", "contradictions", List.of("tese conflitante"))
                        ),
                        Map.of(),
                        Map.of("retainedTurnCount", 3)
                ),
                new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()),
                new LegalAiConversationToolScopeSnapshot(
                        "OPEN",
                        List.of("LEGAL_SEARCH"),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of(
                                "mcpSkillIds", List.of("LEGAL_SKILL_RECURSAL"),
                                "mcpToolExampleIds", List.of("EXAMPLE_RECURSO"),
                                "mcpPromotedToolExampleIds", List.of("EXAMPLE_RECURSO"),
                                "mcpTranscriptReplayReady", false,
                                "mcpBenchmarkPassed", false,
                                "mcpDoctorReady", false,
                                "mcpDoctorStatus", "DEGRADED",
                                "mcpEvidencePromotionStatus", "PROMOTION_HELD",
                                "mcpQualityScore", 61.0d
                        )
                ),
                validation(),
                guard()
        );

        assertEquals("BLOCKED", snapshot.status());
        assertTrue(snapshot.blockedSurface());
        assertTrue(snapshot.driftDetected());
        assertEquals("SESSION_LOCKDOWN", snapshot.operationalMode());
        assertFalse(snapshot.blockedSkillIds().isEmpty());
        assertFalse(snapshot.blockedToolExampleIds().isEmpty());
    }

    private LegalValidationResponse validation() {
        return new LegalValidationResponse(
                "LEGAL_AI_SPINE",
                "V3",
                "LEGAL_VALIDATE_ENVELOPE_V3",
                "VALIDATED",
                true,
                "STEP_UP_REQUIRED",
                List.of("LEGAL_SIGILO_RULE_ENGINE"),
                List.of("grounding"),
                List.of("tese conflitante"),
                List.of("precedente não confirmado"),
                "LEGAL_RESEARCH_DOSSIER_V2",
                Map.of("symbolicExecutionStatus", "WARN")
        );
    }

    private LegalHallucinationGuardResponse guard() {
        return new LegalHallucinationGuardResponse(
                "LEGAL_AI_SPINE",
                "V3",
                "LEGAL_HALLUCINATION_GUARD_V3",
                "ALLOW",
                true,
                true,
                true,
                "GROUNDING_FIRST",
                "[NAO_CONFIRMADO]",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
