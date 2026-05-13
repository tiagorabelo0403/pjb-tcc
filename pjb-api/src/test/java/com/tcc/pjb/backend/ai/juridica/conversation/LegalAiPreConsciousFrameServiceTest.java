package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiPreConsciousFrameServiceTest {

    @Test
    void mustEscalateProceduralAndPhilosophicalFrameBeforeAnswer() {
        var snapshot = service().inspect(
                request("Quero apelação com tutela de urgência e análise constitucional"),
                "LEGAL_RECURSAL_DRAFT_V3",
                "V3",
                memory(),
                clearedDocument(),
                openToolScope(),
                publicTrustZone(),
                promotedEvidence(),
                coverage("PROCESSUAL_CIVIL", "CONSTITUCIONAL"),
                validEnvelope(),
                passedGuard()
        );

        assertEquals("ESCALATED", snapshot.status());
        assertEquals("ASSISTED_GROUNDED_REASONING", snapshot.mode());
        assertEquals("PROCEDURAL_ADMISSIBILITY_REQUIRED", snapshot.authorityFloor());
        assertTrue(snapshot.responseAllowed());
        assertTrue(snapshot.humanReviewRequired());
        assertTrue(snapshot.lineages().stream().anyMatch(item -> "CONSTITUCIONAL".equals(item.branchCode())));
        assertTrue(snapshot.lineages().stream().anyMatch(item -> "PROCESSUAL_CIVIL".equals(item.branchCode())));
        assertTrue(snapshot.dominantLenses().contains("proporcionalidade"));
        assertTrue(snapshot.authorityChecks().contains("cabimento"));
        assertTrue(snapshot.signals().stream().anyMatch(item -> "APPEAL_ADMISSIBILITY_RISK".equals(item.code())));
        assertTrue(snapshot.nextActions().stream().anyMatch(item -> item.contains("piso de autoridade")));
    }

    @Test
    void mustBlockWhenSovereignEvidenceAndGuardConverge() {
        var snapshot = service().inspect(
                request("Faça minuta penal sigilosa com precedente não confirmado"),
                "LEGAL_RECURSAL_DRAFT_V3",
                "V3",
                memory(),
                restrictedDocument(),
                lockedToolScope(),
                lockedTrustZone(),
                lockedEvidence(),
                coverage("PENAL"),
                invalidEnvelope(),
                blockedGuard()
        );

        assertEquals("BLOCKED", snapshot.status());
        assertEquals("SOVEREIGN_PRE_RESPONSE_LOCK", snapshot.mode());
        assertEquals("CITATION_FIRST_BLOCKING", snapshot.authorityFloor());
        assertFalse(snapshot.responseAllowed());
        assertTrue(snapshot.humanReviewRequired());
        assertTrue(snapshot.learningCandidate());
        assertTrue(snapshot.riskScore() >= 80);
        assertTrue(snapshot.signals().stream().anyMatch(item -> "TRUST_ZONE_LOCK".equals(item.code())));
        assertTrue(snapshot.signals().stream().anyMatch(item -> "EVIDENCE_PROVENANCE_LOCK".equals(item.code())));
        assertTrue(snapshot.lineages().stream().anyMatch(item -> "PENAL".equals(item.branchCode())));
    }

    private LegalAiPreConsciousFrameService service() {
        return new LegalAiPreConsciousFrameService(new LegalAiJuridicalLineageRegistry(), new LegalAiPreConsciousSignalExtractor());
    }

    private LegalAiConversationRequest request(String message) {
        return new LegalAiConversationRequest("conv-pre", "PROC-PRE", message, "ADVOGADO", List.of(), List.of("prova.pdf"), Map.of("sigilo", "sigiloso"));
    }

    private LegalAiConversationMemorySnapshot memory() {
        return new LegalAiConversationMemorySnapshot("conv-pre", "PROC-PRE", "ADVOGADO", List.of(), Map.of(), Map.of());
    }

    private LegalAiConversationDocumentSecuritySnapshot clearedDocument() {
        return new LegalAiConversationDocumentSecuritySnapshot("CLEARED", List.of("STJ"), List.of(), List.of("prova.pdf"), List.of(), List.of(), Map.of());
    }

    private LegalAiConversationDocumentSecuritySnapshot restrictedDocument() {
        return new LegalAiConversationDocumentSecuritySnapshot("HUMAN_REVIEW_REQUIRED", List.of(), List.of("fonte-externa"), List.of(), List.of("prova.pdf"), List.of("QUARANTINE"), Map.of());
    }

    private LegalAiConversationToolScopeSnapshot openToolScope() {
        return new LegalAiConversationToolScopeSnapshot("OPEN", List.of("LEGAL_RAG_SEARCH"), List.of(), List.of(), List.of(), Map.of());
    }

    private LegalAiConversationToolScopeSnapshot lockedToolScope() {
        return new LegalAiConversationToolScopeSnapshot("TRUST_ZONE_LOCKED", List.of("LEGAL_RAG_SEARCH"), List.of("LEGAL_DRAFT"), List.of(), List.of(), Map.of());
    }

    private LegalAiConversationTrustZoneSnapshot publicTrustZone() {
        return new LegalAiConversationTrustZoneSnapshot("NOT_REQUIRED", "PUBLIC", false, true, "PUBLIC_INTERNAL", "ALLOWLISTED", "READ_ONLY", "PUBLIC_READ_ONLY", List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    private LegalAiConversationTrustZoneSnapshot lockedTrustZone() {
        return new LegalAiConversationTrustZoneSnapshot("LOCKED", "CRITICAL", true, true, "BLOCKED_EXTERNAL", "QUARANTINED", "MUTATING", "SOVEREIGN_HARD_LOCK", List.of("LEGAL_DRAFT"), List.of(), List.of("SIGILO_SENSITIVE_FLOW"), List.of(), Map.of());
    }

    private LegalAiConversationEvidenceProvenanceSnapshot promotedEvidence() {
        return new LegalAiConversationEvidenceProvenanceSnapshot("ENFORCED", "OFFICIAL_DOCUMENT", "OFFICIAL", "ALLOWLISTED", "OFFICIAL_CHAIN", "PROMOTED", "PROMOTED", "GATED", "PROMOTED", "PROMOTED", List.of("STJ"), List.of(), List.of(), List.of(), List.of(), List.of("STJ"), List.of("STJ"), List.of(), List.of("STJ"), List.of("STJ"), List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    private LegalAiConversationEvidenceProvenanceSnapshot lockedEvidence() {
        return new LegalAiConversationEvidenceProvenanceSnapshot("LOCKED", "UNTRUSTED_DOCUMENT", "BLOCKED_EXTERNAL", "QUARANTINED", "SOVEREIGN_HARD_LOCK", "BLOCKED", "BLOCKED", "BLOCKED", "BLOCKED", "BLOCKED", List.of(), List.of(), List.of(), List.of("prova.pdf"), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("LEGAL_DRAFT"), List.of(), List.of("EVIDENCE_CHAIN_REQUIRED"), List.of(), Map.of());
    }

    private LegalKnowledgeCoverageSnapshot coverage(String... branches) {
        return new LegalKnowledgeCoverageSnapshot("OFFICIAL_READY", "OFFICIAL_PRIMARY_ONLY", List.of(branches), List.of(), List.of(), List.of("legislacao", "jurisprudencia"), List.of("citation-first"), List.of(), Map.of());
    }

    private LegalValidationResponse validEnvelope() {
        return new LegalValidationResponse("LEGAL", "V3", "LEGAL_RECURSAL_DRAFT_V3", "VALID", true, "STEP_UP", List.of(), List.of(), List.of(), List.of(), "LEGAL_DRAFT", Map.of());
    }

    private LegalValidationResponse invalidEnvelope() {
        return new LegalValidationResponse("LEGAL", "V3", "LEGAL_RECURSAL_DRAFT_V3", "REVIEW", true, "HUMAN_REVIEW", List.of(), List.of(), List.of("precedente obrigatório não confirmado"), List.of("fonte externa conflita com sigilo"), "LEGAL_DRAFT", Map.of());
    }

    private LegalHallucinationGuardResponse passedGuard() {
        return new LegalHallucinationGuardResponse("LEGAL", "V3", "LEGAL_RECURSAL_DRAFT_V3", "PASSED", true, true, true, "CITATION_FIRST", "[NAO_CONFIRMADO]", "ENFORCED", "OFFICIAL_DOCUMENT", "OFFICIAL_CHAIN", "PROMOTED", List.of(), List.of(), Map.of());
    }

    private LegalHallucinationGuardResponse blockedGuard() {
        return new LegalHallucinationGuardResponse("LEGAL", "V3", "LEGAL_RECURSAL_DRAFT_V3", "BLOCKED", true, true, true, "CITATION_FIRST", "[NAO_CONFIRMADO]", "LOCKED", "UNTRUSTED_DOCUMENT", "SOVEREIGN_HARD_LOCK", "BLOCKED", List.of("precedente sem fonte"), List.of("grounding insuficiente"), Map.of());
    }
}
