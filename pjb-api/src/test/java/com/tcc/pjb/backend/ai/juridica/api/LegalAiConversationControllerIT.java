package com.tcc.pjb.backend.ai.juridica.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LegalAiConversationControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiConversationController(conversationService)).build();
    }

    @Test
    void converseMustExposeDedicatedConversationSurfaceStates() throws Exception {
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
                .andExpect(jsonPath("$.safeguards.evidenceProvenanceTier").value("OFFICIAL_DOCUMENT"));

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safeguards.approvalStatus").value("STEP_UP_REQUIRED"))
                .andExpect(jsonPath("$.safeguards.trustZone").value("SIGILOSA"));

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safeguards.approvalStatus").value("HUMAN_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.safeguards.trustZoneStatus").value("LOCKED"))
                .andExpect(jsonPath("$.conversationContext.conversationTrustZone.trustZone").value("CRITICAL"));
    }
}
