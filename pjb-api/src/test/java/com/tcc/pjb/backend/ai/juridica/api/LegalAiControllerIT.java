package com.tcc.pjb.backend.ai.juridica.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LegalAiControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiController(surfaceFacadeService), new LegalAiConversationController(conversationService)).build();
    }

    @Test
    void minutaMustExposePromotedStepUpAndBlockedStatesOverHttp() throws Exception {
        when(surfaceFacadeService.minuta(any()))
                .thenReturn(new LegalDraftResponse(
                        "RELATÓRIO\nMinuta promovida por evidência oficial.",
                        "PROMOTED",
                        List.of("Evidência oficial soberana confirmada."),
                        Map.of(
                                "surfaceStatus", "NOT_REQUIRED",
                                "draftPromotionStatus", "PROMOTED",
                                "promotedDraftEvidenceIds", List.of("CNJ-OK")
                        )))
                .thenReturn(new LegalDraftResponse(
                        "RELATÓRIO\n[PENDENTE_CONFIRMACAO_SOBERANA]",
                        "STEP_UP_REQUIRED",
                        List.of("Confirmar cadeia oficial antes da redação material."),
                        Map.of(
                                "surfaceStatus", "ESCALATED",
                                "draftPromotionStatus", "STEP_UP_REQUIRED",
                                "promotedDraftEvidenceIds", List.of()
                        )))
                .thenReturn(new LegalDraftResponse(
                        "RELATÓRIO\n[PROMOCAO_DOCUMENTAL_BLOQUEADA]",
                        "BLOCKED",
                        List.of("Remover anexo em quarentena e reenviar origem oficial."),
                        Map.of(
                                "surfaceStatus", "LOCKED",
                                "draftPromotionStatus", "BLOCKED",
                                "promotedDraftEvidenceIds", List.of()
                        )));

        LegalDraftRequest request = new LegalDraftRequest(
                "Análise v1",
                "Texto inicial",
                "Instruções",
                "Gerar minuta",
                "ADVOGADO",
                "PROC-1",
                "CIVEL",
                "COMUM",
                "OBRIGACAO_DE_FAZER",
                List.of("documento.pdf"),
                Map.of("sourceSystem", "CNJ")
        );

        mockMvc.perform(post("/api/ai/legal/minuta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROMOTED"))
                .andExpect(jsonPath("$.safeguards.draftPromotionStatus").value("PROMOTED"))
                .andExpect(jsonPath("$.safeguards.promotedDraftEvidenceIds[0]").value("CNJ-OK"));

        mockMvc.perform(post("/api/ai/legal/minuta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STEP_UP_REQUIRED"))
                .andExpect(jsonPath("$.safeguards.draftPromotionStatus").value("STEP_UP_REQUIRED"))
                .andExpect(jsonPath("$.nextSteps[0]").value("Confirmar cadeia oficial antes da redação material."));

        mockMvc.perform(post("/api/ai/legal/minuta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.safeguards.draftPromotionStatus").value("BLOCKED"))
                .andExpect(jsonPath("$.minuta").value(org.hamcrest.Matchers.containsString("PROMOCAO_DOCUMENTAL_BLOQUEADA")));
    }

    @Test
    void groundingCheckMustExposePromotedReviewRequiredAndBlockedStatesOverHttp() throws Exception {
        when(surfaceFacadeService.hallucinationGuard(any()))
                .thenReturn(new LegalHallucinationGuardResponse(
                        "LEGAL_GROUNDING_CHECK_V3",
                        "V3",
                        "LEGAL_GROUNDING_CHECK_V3",
                        "ALIGNED",
                        true,
                        true,
                        true,
                        "STRICT_OFFICIAL_ONLY",
                        "[CITACAO_PENDENTE_VALIDACAO]",
                        "NOT_REQUIRED",
                        "OFFICIAL_DOCUMENT",
                        "SOVEREIGN_PROMOTED",
                        "PROMOTED",
                        List.of(),
                        List.of(),
                        Map.of("surfaceStatus", "NOT_REQUIRED", "promotedGroundingEvidenceIds", List.of("CNJ-OK"))))
                .thenReturn(new LegalHallucinationGuardResponse(
                        "LEGAL_GROUNDING_CHECK_V3",
                        "V3",
                        "LEGAL_GROUNDING_CHECK_V3",
                        "REVIEW_REQUIRED",
                        true,
                        true,
                        true,
                        "STRICT_OFFICIAL_ONLY",
                        "[CITACAO_PENDENTE_VALIDACAO]",
                        "ESCALATED",
                        "DERIVED_DOCUMENT",
                        "SOVEREIGN_STEP_UP",
                        "STEP_UP_REQUIRED",
                        List.of("A cadeia soberana exige confirmação oficial adicional."),
                        List.of(),
                        Map.of("surfaceStatus", "ESCALATED", "promotedGroundingEvidenceIds", List.of())))
                .thenReturn(new LegalHallucinationGuardResponse(
                        "LEGAL_GROUNDING_CHECK_V3",
                        "V3",
                        "LEGAL_GROUNDING_CHECK_V3",
                        "BLOCKED",
                        true,
                        true,
                        true,
                        "STRICT_OFFICIAL_ONLY",
                        "[CITACAO_PENDENTE_VALIDACAO]",
                        "LOCKED",
                        "UNTRUSTED_DOCUMENT",
                        "SOVEREIGN_PROVENANCE_HARD_LOCK",
                        "BLOCKED",
                        List.of(),
                        List.of("Grounding estruturado foi bloqueado pela cerca soberana."),
                        Map.of("surfaceStatus", "LOCKED", "promotedGroundingEvidenceIds", List.of())));

        LegalHallucinationGuardRequest request = new LegalHallucinationGuardRequest(
                "Verificar consistência das citações do acórdão.",
                "CIVEL",
                "COMUM",
                "OBRIGACAO_DE_FAZER",
                List.of("AgInt no REsp 1"),
                Map.of("sourceSystem", "CNJ")
        );

        mockMvc.perform(post("/api/ai/legal/grounding/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALIGNED"))
                .andExpect(jsonPath("$.groundingPromotionStatus").value("PROMOTED"))
                .andExpect(jsonPath("$.trace.promotedGroundingEvidenceIds[0]").value("CNJ-OK"));

        mockMvc.perform(post("/api/ai/legal/grounding/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.groundingPromotionStatus").value("STEP_UP_REQUIRED"))
                .andExpect(jsonPath("$.suspiciousSignals[0]").value("A cadeia soberana exige confirmação oficial adicional."));

        mockMvc.perform(post("/api/ai/legal/grounding/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.groundingPromotionStatus").value("BLOCKED"))
                .andExpect(jsonPath("$.blockedReasons[0]").value("Grounding estruturado foi bloqueado pela cerca soberana."));
    }

    @Test
    void conversationMustExposeApprovalTrustZoneAndEvidenceProvenanceOverHttp() throws Exception {
        when(conversationService.converse(any()))
                .thenReturn(new LegalAiConversationResponse(
                        "conv-promoted",
                        "V3",
                        "LEGAL_GENERAL_ASSIST_V3",
                        "Resposta conversacional jurídica V3: lastro oficial confirmado.",
                        List.of("Manter a conversa em modo read-only soberano."),
                        List.of(Map.of("virtualTrend", "VT_OFFICIAL_GROUNDING", "action", "Manter lastro oficial confirmado.")),
                        Map.of(
                                "conversationApproval", Map.of("status", "AUTO_READONLY"),
                                "conversationTrustZone", Map.of("status", "NOT_REQUIRED", "trustZone", "PUBLIC"),
                                "conversationEvidenceProvenance", Map.of("status", "NOT_REQUIRED", "tier", "OFFICIAL_DOCUMENT")
                        ),
                        Map.of(
                                "approvalStatus", "AUTO_READONLY",
                                "trustZoneStatus", "NOT_REQUIRED",
                                "trustZone", "PUBLIC",
                                "evidenceProvenanceStatus", "NOT_REQUIRED",
                                "evidenceProvenanceTier", "OFFICIAL_DOCUMENT"
                        )))
                .thenReturn(new LegalAiConversationResponse(
                        "conv-step-up",
                        "V3",
                        "LEGAL_GENERAL_ASSIST_V3",
                        "Resposta conversacional jurídica V3: confirmação soberana adicional necessária.",
                        List.of("Exigir confirmação oficial adicional antes de ampliar o grounding desta conversa."),
                        List.of(Map.of("virtualTrend", "VT_SIGILO_STEP_UP", "action", "Exigir confirmação soberana adicional.")),
                        Map.of(
                                "conversationApproval", Map.of("status", "STEP_UP_REQUIRED"),
                                "conversationTrustZone", Map.of("status", "ESCALATED", "trustZone", "SIGILOSA"),
                                "conversationEvidenceProvenance", Map.of("status", "ESCALATED", "tier", "DERIVED_DOCUMENT")
                        ),
                        Map.of(
                                "approvalStatus", "STEP_UP_REQUIRED",
                                "trustZoneStatus", "ESCALATED",
                                "trustZone", "SIGILOSA",
                                "evidenceProvenanceStatus", "ESCALATED",
                                "evidenceProvenanceTier", "DERIVED_DOCUMENT"
                        )))
                .thenReturn(new LegalAiConversationResponse(
                        "conv-blocked",
                        "V3",
                        "LEGAL_GENERAL_ASSIST_V3",
                        "Resposta conversacional jurídica V3: conversa retida para revisão humana soberana.",
                        List.of("Substituir anexo em quarentena e reenviar apenas origem oficial controlada."),
                        List.of(Map.of("virtualTrend", "VT_CRITICAL_LOCK", "action", "Bloquear emissão material até revisão humana.")),
                        Map.of(
                                "conversationApproval", Map.of("status", "HUMAN_REVIEW_REQUIRED"),
                                "conversationTrustZone", Map.of("status", "LOCKED", "trustZone", "CRITICAL"),
                                "conversationEvidenceProvenance", Map.of("status", "LOCKED", "tier", "UNTRUSTED_DOCUMENT")
                        ),
                        Map.of(
                                "approvalStatus", "HUMAN_REVIEW_REQUIRED",
                                "trustZoneStatus", "LOCKED",
                                "trustZone", "CRITICAL",
                                "evidenceProvenanceStatus", "LOCKED",
                                "evidenceProvenanceTier", "UNTRUSTED_DOCUMENT"
                        )));

        LegalAiConversationRequest request = new LegalAiConversationRequest(
                "conv-http",
                "PROC-1",
                "Preciso validar o cabimento recursal com base no acórdão anexado.",
                "ADVOGADO",
                List.of("Contexto anterior"),
                List.of("acordao.pdf"),
                Map.of("sourceSystem", "CNJ", "sigilo", "publico")
        );

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safeguards.approvalStatus").value("AUTO_READONLY"))
                .andExpect(jsonPath("$.safeguards.trustZoneStatus").value("NOT_REQUIRED"))
                .andExpect(jsonPath("$.safeguards.evidenceProvenanceTier").value("OFFICIAL_DOCUMENT"))
                .andExpect(jsonPath("$.conversationContext.conversationApproval.status").value("AUTO_READONLY"));

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safeguards.approvalStatus").value("STEP_UP_REQUIRED"))
                .andExpect(jsonPath("$.safeguards.trustZone").value("SIGILOSA"))
                .andExpect(jsonPath("$.safeguards.evidenceProvenanceStatus").value("ESCALATED"))
                .andExpect(jsonPath("$.nextSteps[0]").value("Exigir confirmação oficial adicional antes de ampliar o grounding desta conversa."));

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safeguards.approvalStatus").value("HUMAN_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.safeguards.trustZoneStatus").value("LOCKED"))
                .andExpect(jsonPath("$.safeguards.evidenceProvenanceTier").value("UNTRUSTED_DOCUMENT"))
                .andExpect(jsonPath("$.conversationContext.conversationTrustZone.trustZone").value("CRITICAL"));
    }

}
