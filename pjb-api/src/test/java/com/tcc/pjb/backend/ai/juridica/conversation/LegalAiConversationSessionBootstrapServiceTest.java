package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationSessionBootstrapServiceTest {

    @Test
    void mustBlockCriticalCapabilityForCitizenUnderSigiloAndRepeatedDrift() {
        var service = new LegalAiConversationSessionBootstrapService();
        var snapshot = service.inspect(
                new LegalAiConversationRequest(
                        "conv-119",
                        "PROC-119",
                        "Quero minuta recursal sigilosa",
                        "CIDADAO",
                        List.of("turno 1", "turno 2", "turno 3"),
                        List.of(),
                        Map.of("sigilo", "SIGILOSO")
                ),
                "LEGAL_RECURSAL_ASSIST_V3",
                "V3",
                new LegalAiConversationMemorySnapshot(
                        "conv-119",
                        "PROC-119",
                        "CIDADAO",
                        List.of(Map.of(), Map.of(), Map.of()),
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
                                "mcpSkillIds", List.of(),
                                "mcpToolExampleIds", List.of(),
                                "mcpPromotedToolExampleIds", List.of()
                        )
                ),
                new LegalAiConversationSessionDoctorSnapshot(
                        "DEGRADED",
                        false,
                        true,
                        "SESSION_MONITORED_REPLAY_FROZEN",
                        List.of("LEGAL_SKILL_RECURSAL"),
                        List.of("EXAMPLE_RECURSO"),
                        List.of("Contradição relevante", "Insuficiência probatória", "Insuficiência normativa"),
                        Map.of()
                )
        );

        assertEquals("BLOCKED", snapshot.status());
        assertTrue(snapshot.blockedCapability());
        assertTrue(snapshot.repeatedDriftDetected());
        assertEquals("BLOCKED", snapshot.profileGate());
        assertEquals("BLOCKED", snapshot.sigiloFence());
        assertFalse(snapshot.missingSkillIds().isEmpty());
        assertFalse(snapshot.missingToolExampleIds().isEmpty());
    }
}
