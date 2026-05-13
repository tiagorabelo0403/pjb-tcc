package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiJuridicalLineageDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiPreConsciousFrameSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiPreConsciousSignal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiPreConsciousToolScopeEnricherTest {

    @Test
    void mustBlockAllowedToolsWhenPreConsciousFrameBlocksResponse() {
        var snapshot = new LegalAiConversationToolScopeSnapshot(
                "OPEN",
                List.of("LEGAL_DRAFT", "LEGAL_RAG_SEARCH"),
                List.of(),
                List.of("LEGAL_DRAFT"),
                List.of(),
                Map.of()
        );

        var result = new LegalAiPreConsciousToolScopeEnricher().enrich(snapshot, blockedFrame());

        assertEquals("PRE_CONSCIOUS_BLOCKED", result.status());
        assertTrue(result.allowedToolIds().isEmpty());
        assertTrue(result.stepUpToolIds().isEmpty());
        assertTrue(result.blockedToolIds().contains("LEGAL_DRAFT"));
        assertTrue(result.blockedToolIds().contains("LEGAL_RAG_SEARCH"));
        assertEquals("BLOCKED", result.diagnostics().get("preConsciousStatus"));
        assertEquals(100, result.diagnostics().get("preConsciousRiskScore"));
    }

    @Test
    void mustGateAllowedToolsWhenHumanReviewIsRequired() {
        var snapshot = new LegalAiConversationToolScopeSnapshot(
                "OPEN",
                List.of("LEGAL_GROUNDING", "LEGAL_RAG_SEARCH"),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        var result = new LegalAiPreConsciousToolScopeEnricher().enrich(snapshot, escalatedFrame());

        assertEquals("PRE_CONSCIOUS_GATED", result.status());
        assertTrue(result.stepUpToolIds().contains("LEGAL_GROUNDING"));
        assertTrue(result.stepUpToolIds().contains("LEGAL_RAG_SEARCH"));
        assertEquals("ESCALATED", result.diagnostics().get("preConsciousStatus"));
        assertTrue(result.reasons().stream().anyMatch(item -> item.contains("revisão assistida")));
    }

    private LegalAiPreConsciousFrameSnapshot blockedFrame() {
        return new LegalAiPreConsciousFrameSnapshot(
                "BLOCKED",
                "SOVEREIGN_PRE_RESPONSE_LOCK",
                "CITATION_FIRST_BLOCKING",
                "INHIBITORY_CONTROL_ACTIVE",
                100,
                false,
                true,
                true,
                List.of(lineage()),
                List.of(LegalAiPreConsciousSignal.critical("HALLUCINATION_GUARD_BLOCK", "Guard bloqueado", "TEST")),
                List.of("riskScore"),
                List.of("garantismo"),
                List.of("fonte normativa"),
                List.of("Bloquear emissão operacional."),
                Map.of()
        );
    }

    private LegalAiPreConsciousFrameSnapshot escalatedFrame() {
        return new LegalAiPreConsciousFrameSnapshot(
                "ESCALATED",
                "ASSISTED_GROUNDED_REASONING",
                "PROCEDURAL_ADMISSIBILITY_REQUIRED",
                "REFLECTIVE_SYSTEM_ACTIVE",
                55,
                true,
                true,
                false,
                List.of(lineage()),
                List.of(LegalAiPreConsciousSignal.high("APPEAL_ADMISSIBILITY_RISK", "Cabimento recursal", "TEST")),
                List.of("riskScore"),
                List.of("contraditório substancial"),
                List.of("cabimento"),
                List.of("Checar cabimento."),
                Map.of()
        );
    }

    private LegalAiJuridicalLineageDescriptor lineage() {
        return new LegalAiJuridicalLineageDescriptor(
                "PROCESSUAL_CIVIL",
                "Direito Processual Civil",
                "processo como garantia",
                List.of("Giuseppe Chiovenda"),
                List.of("Fredie Didier Jr."),
                List.of("contraditório substancial"),
                List.of("cabimento")
        );
    }
}
