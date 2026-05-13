package com.tcc.pjb.backend.ai.juridica.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LegalAiSelectiveSignedOriginGovernanceIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var perimeterProperties = LegalAiSignedOriginTestSupport.perimeterProperties();
        var clientIpResolver = LegalAiSignedOriginTestSupport.clientIpResolver(perimeterProperties);
        when(conversationService.converse(any())).thenReturn(new LegalAiConversationResponse(
                "conv-selective",
                "V3",
                "LEGAL_GENERAL_ASSIST_V3",
                "Resposta juridica governada pela politica seletiva de origem.",
                List.of("Preservar a cerca soberana por capability sensivel."),
                List.of(Map.of("virtualTrend", "VT_SELECTIVE_SIGNED", "action", "Exigir canal assinado apenas nas capabilities sensiveis.")),
                Map.of("conversationApproval", Map.of("status", "AUTO_READONLY")),
                Map.of("approvalStatus", "AUTO_READONLY")
        ));
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiController(surfaceFacadeService), new LegalAiConversationController(conversationService))
                .addFilters(
                        LegalAiSignedOriginTestSupport.apiRouteGovernanceFilter(clientIpResolver),
                        LegalAiSignedOriginTestSupport.requestBodyHashFilter(objectMapper),
                        LegalAiSignedOriginTestSupport.selectiveConversationOriginGovernanceFilter(perimeterProperties, clientIpResolver, objectMapper)
                )
                .build();
    }

    @Test
    void legalAiConversationMustAcceptTrustedBrowserOnNonSensitiveCapability() throws Exception {
        String body = LegalAiSignedOriginTestSupport.legalConversationRequestBody(objectMapper, "LEGAL_GENERAL_ASSIST_V3");

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Origin", "https://app.pjb.justica.br"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-PJB-Origin-Mode", "BROWSER_ALLOWLIST"))
                .andExpect(header().string("X-PJB-Origin-Requirement", "BROWSER_OR_SIGNED"))
                .andExpect(header().string("X-PJB-Origin-Capability", "LEGAL_GENERAL_ASSIST_V3"))
                .andExpect(jsonPath("$.conversationId").value("conv-selective"));
    }

    @Test
    void legalAiConversationMustRequireSignedAttestationOnSensitiveCapabilityEvenWithTrustedBrowser() throws Exception {
        String body = LegalAiSignedOriginTestSupport.legalConversationRequestBody(objectMapper, "LEGAL_DRAFT_V2");

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Origin", "https://app.pjb.justica.br"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/signed_attestation_required_for_capability"));
    }

    @Test
    void legalAiConversationMustAcceptSensitiveCapabilityWhenSignedAttestationIsValid() throws Exception {
        String body = LegalAiSignedOriginTestSupport.legalConversationRequestBody(objectMapper, "LEGAL_DRAFT_V2");
        var signed = LegalAiSignedOriginTestSupport.signedRequest("/api/ai/legal/conversation", body);

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("Origin", "https://app.pjb.justica.br")
                        .header("X-PJB-Origin-Id", signed.originId())
                        .header("X-PJB-Timestamp", signed.timestamp())
                        .header("X-PJB-Body-Hash", signed.bodyHash())
                        .header("X-PJB-Signature", signed.signature()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-PJB-Origin-Mode", "SIGNED_ATTESTATION"))
                .andExpect(header().string("X-PJB-Origin-Requirement", "SIGNED_REQUIRED"))
                .andExpect(header().string("X-PJB-Origin-Capability", "LEGAL_DRAFT_V2"))
                .andExpect(jsonPath("$.answer").value("Resposta juridica governada pela politica seletiva de origem."));
    }
}
