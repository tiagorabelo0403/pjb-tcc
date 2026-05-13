package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTraceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiMemoryScopeDescriptor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationMemoryServiceTest {

    @Test
    void mustRetainTurnsAndResetProcessScopeWhenCrossCaseReuseIsBlocked() {
        var service = new LegalAiConversationMemoryService();
        var descriptor = new LegalAiMemoryScopeDescriptor(
                List.of("INSTITUTIONAL", "PROCESSO", "PERFIL", "SESSAO"),
                true,
                true,
                Map.of("sessionTtlMinutes", 45, "retainedTurnWindow", 8)
        );
        var requestA = new LegalAiConversationRequest(
                "conv-memory",
                "PROC-A",
                "Mensagem do processo A",
                "ADVOGADO",
                List.of(),
                List.of(),
                Map.of("ramo", "civel", "rito", "comum", "classe", "apelacao", "institutionId", "TJCE")
        );
        var requestB = new LegalAiConversationRequest(
                "conv-memory",
                "PROC-B",
                "Mensagem do processo B",
                "ADVOGADO",
                List.of(),
                List.of(),
                Map.of("ramo", "penal", "rito", "comum", "classe", "apelação criminal", "institutionId", "TJCE")
        );

        service.registerTurn("conv-memory", requestA, "LEGAL_CHAT_VALIDATION_V2", "V2", "resposta A", validation(), guard(), approval(), trace("turn-1", "trace-1"), List.of(), descriptor);
        var afterFirstTurn = service.snapshot("conv-memory", requestA, descriptor);
        assertEquals(1, afterFirstTurn.retainedTurns().size());
        assertEquals("PROC-A", ((Map<?, ?>) afterFirstTurn.scopedMemory().get("PROCESSO")).get("processoId"));

        service.registerTurn("conv-memory", requestB, "LEGAL_CHAT_VALIDATION_V2", "V2", "resposta B", validation(), guard(), approval(), trace("turn-2", "trace-2"), List.of(), descriptor);
        var afterSecondTurn = service.snapshot("conv-memory", requestB, descriptor);
        assertEquals("PROC-B", ((Map<?, ?>) afterSecondTurn.scopedMemory().get("PROCESSO")).get("processoId"));
        assertEquals(1, afterSecondTurn.retainedTurns().size());
        assertTrue(afterSecondTurn.retainedTurns().stream().allMatch(turn -> "PROC-B".equals(turn.get("processoId"))));
    }

    private LegalValidationResponse validation() {
        return new LegalValidationResponse(
                "LEGAL_AI_SPINE",
                "V3",
                "LEGAL_VALIDATE_ENVELOPE_V3",
                "VALIDATED",
                true,
                "AUTO_READONLY",
                List.of("LEGAL_PRAZO_RULE_ENGINE"),
                List.of("grounding"),
                List.of(),
                List.of(),
                "LEGAL_RESEARCH_DOSSIER_V2",
                Map.of("symbolicExecutionStatus", "PASS")
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

    private LegalAiConversationApprovalSnapshot approval() {
        return new LegalAiConversationApprovalSnapshot(
                "AUTO_READONLY",
                false,
                false,
                List.of("AUTO_READONLY"),
                List.of("Manter a conversa em trilha read-only até aprovação material."),
                Map.of()
        );
    }

    private LegalAiConversationTraceSnapshot trace(String turnId, String traceId) {
        return new LegalAiConversationTraceSnapshot(traceId, turnId, "LEGAL_DECISION_TRACE", "COMPLETED", List.of("requestId"), Map.of(), List.of());
    }
}
