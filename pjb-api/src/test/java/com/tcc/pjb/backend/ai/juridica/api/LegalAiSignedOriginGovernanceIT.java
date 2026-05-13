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
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LegalAiSignedOriginGovernanceIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var perimeterProperties = LegalAiSignedOriginTestSupport.perimeterProperties();
        var clientIpResolver = LegalAiSignedOriginTestSupport.clientIpResolver(perimeterProperties);
        stubResponses();
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiController(surfaceFacadeService), new LegalAiConversationController(conversationService))
                .addFilters(
                        LegalAiSignedOriginTestSupport.apiRouteGovernanceFilter(clientIpResolver),
                        LegalAiSignedOriginTestSupport.requestBodyHashFilter(objectMapper),
                        LegalAiSignedOriginTestSupport.signedRequiredOriginGovernanceFilter(perimeterProperties, clientIpResolver, objectMapper)
                )
                .build();
    }

    @Test
    void legalAiGovernedRoutesMustAcceptTrustedSignedAttestation() throws Exception {
        String draftBody = LegalAiSignedOriginTestSupport.legalDraftRequestBody(objectMapper);
        var draftSigned = LegalAiSignedOriginTestSupport.signedRequest("/api/ai/legal/minuta", draftBody);
        mockMvc.perform(post("/api/ai/legal/minuta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("X-PJB-Origin-Id", draftSigned.originId())
                        .header("X-PJB-Timestamp", draftSigned.timestamp())
                        .header("X-PJB-Body-Hash", draftSigned.bodyHash())
                        .header("X-PJB-Signature-Alg", "HMAC-SHA256")
                        .header("X-PJB-Signature", draftSigned.signature()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-PJB-Origin-Mode", "SIGNED_ATTESTATION"))
                .andExpect(header().string("X-PJB-Origin-Subject", LegalAiSignedOriginTestSupport.TRUSTED_ORIGIN_ID))
                .andExpect(jsonPath("$.status").value("PROMOTED"));

        String groundingBody = LegalAiSignedOriginTestSupport.legalGroundingRequestBody(objectMapper);
        var groundingSigned = LegalAiSignedOriginTestSupport.signedRequest("/api/ai/legal/grounding/check", groundingBody);
        mockMvc.perform(post("/api/ai/legal/grounding/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groundingBody)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("X-PJB-Origin-Id", groundingSigned.originId())
                        .header("X-PJB-Timestamp", groundingSigned.timestamp())
                        .header("X-PJB-Body-Hash", groundingSigned.bodyHash())
                        .header("X-PJB-Signature-Alg", "HMAC-SHA256")
                        .header("X-PJB-Signature", groundingSigned.signature()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-PJB-Origin-Mode", "SIGNED_ATTESTATION"))
                .andExpect(jsonPath("$.groundingPromotionStatus").value("PROMOTED"));

        String conversationBody = LegalAiSignedOriginTestSupport.legalConversationRequestBody(objectMapper);
        var conversationSigned = LegalAiSignedOriginTestSupport.signedRequest("/api/ai/legal/conversation", conversationBody);
        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conversationBody)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("X-PJB-Origin-Id", conversationSigned.originId())
                        .header("X-PJB-Timestamp", conversationSigned.timestamp())
                        .header("X-PJB-Body-Hash", conversationSigned.bodyHash())
                        .header("X-PJB-Signature-Alg", "HMAC-SHA256")
                        .header("X-PJB-Signature", conversationSigned.signature()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-PJB-Origin-Mode", "SIGNED_ATTESTATION"))
                .andExpect(jsonPath("$.safeguards.approvalStatus").value("AUTO_READONLY"));
    }

    @Test
    void legalAiGovernedRoutesMustRejectSignedAttestationWithoutOriginId() throws Exception {
        String body = LegalAiSignedOriginTestSupport.legalConversationRequestBody(objectMapper);
        var signed = LegalAiSignedOriginTestSupport.signedRequest("/api/ai/legal/conversation", body);

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("X-PJB-Timestamp", signed.timestamp())
                        .header("X-PJB-Body-Hash", signed.bodyHash())
                        .header("X-PJB-Signature", signed.signature()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/signed_origin_id_required"));
    }

    @Test
    void legalAiGovernedRoutesMustRejectSignedAttestationWithInvalidSignatureOrTimestampOrBodyHash() throws Exception {
        String groundingBody = LegalAiSignedOriginTestSupport.legalGroundingRequestBody(objectMapper);
        var invalidSignature = LegalAiSignedOriginTestSupport.signedRequest(
                "/api/ai/legal/grounding/check",
                Instant.now(),
                LegalAiSignedOriginTestSupport.signedRequest("/api/ai/legal/grounding/check", groundingBody).bodyHash(),
                LegalAiSignedOriginTestSupport.TRUSTED_ORIGIN_ID,
                "wrong-secret"
        );
        mockMvc.perform(post("/api/ai/legal/grounding/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groundingBody)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("X-PJB-Origin-Id", invalidSignature.originId())
                        .header("X-PJB-Timestamp", invalidSignature.timestamp())
                        .header("X-PJB-Body-Hash", invalidSignature.bodyHash())
                        .header("X-PJB-Signature", invalidSignature.signature()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/signed_origin_signature_invalid"));

        String draftBody = LegalAiSignedOriginTestSupport.legalDraftRequestBody(objectMapper);
        var staleTimestamp = LegalAiSignedOriginTestSupport.signedRequest(
                "/api/ai/legal/minuta",
                Instant.now().minus(10, ChronoUnit.MINUTES),
                LegalAiSignedOriginTestSupport.signedRequest("/api/ai/legal/minuta", draftBody).bodyHash(),
                LegalAiSignedOriginTestSupport.TRUSTED_ORIGIN_ID,
                LegalAiSignedOriginTestSupport.TRUSTED_SECRET
        );
        mockMvc.perform(post("/api/ai/legal/minuta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("X-PJB-Origin-Id", staleTimestamp.originId())
                        .header("X-PJB-Timestamp", staleTimestamp.timestamp())
                        .header("X-PJB-Body-Hash", staleTimestamp.bodyHash())
                        .header("X-PJB-Signature", staleTimestamp.signature()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/signed_origin_timestamp_invalid"));

        String conversationBody = LegalAiSignedOriginTestSupport.legalConversationRequestBody(objectMapper);
        var wrongBodyHash = LegalAiSignedOriginTestSupport.signedRequest(
                "/api/ai/legal/conversation",
                Instant.now(),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                LegalAiSignedOriginTestSupport.TRUSTED_ORIGIN_ID,
                LegalAiSignedOriginTestSupport.TRUSTED_SECRET
        );
        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conversationBody)
                        .with(request -> {
                            request.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
                            return request;
                        })
                        .header("X-PJB-Origin-Id", wrongBodyHash.originId())
                        .header("X-PJB-Timestamp", wrongBodyHash.timestamp())
                        .header("X-PJB-Body-Hash", wrongBodyHash.bodyHash())
                        .header("X-PJB-Signature", wrongBodyHash.signature()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BODY_HASH_MISMATCH"))
                .andExpect(jsonPath("$.message").value("Body hash não confere."));
    }

    private void stubResponses() {
        when(surfaceFacadeService.minuta(any())).thenReturn(new LegalDraftResponse(
                "RELATÓRIO\nMinuta promovida por canal assinado.",
                "PROMOTED",
                List.of("Canal soberano assinado confirmado."),
                Map.of("draftPromotionStatus", "PROMOTED", "signedOrigin", LegalAiSignedOriginTestSupport.TRUSTED_ORIGIN_ID)
        ));
        when(surfaceFacadeService.hallucinationGuard(any())).thenReturn(new LegalHallucinationGuardResponse(
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
                Map.of("promotedGroundingEvidenceIds", List.of("CNJ-OK"), "signedOrigin", LegalAiSignedOriginTestSupport.TRUSTED_ORIGIN_ID)
        ));
        when(conversationService.converse(any())).thenReturn(new LegalAiConversationResponse(
                "conv-signed",
                "V3",
                "LEGAL_GENERAL_ASSIST_V3",
                "Resposta jurídica com origem soberana assinada confirmada.",
                List.of("Manter o canal assinado para atos mutáveis sensíveis."),
                List.of(Map.of("virtualTrend", "VT_SIGNED_CHANNEL", "action", "Preservar origem soberana assinada.")),
                Map.of("conversationApproval", Map.of("status", "AUTO_READONLY")),
                Map.of("approvalStatus", "AUTO_READONLY", "signedOrigin", LegalAiSignedOriginTestSupport.TRUSTED_ORIGIN_ID)
        ));
    }
}
