package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.State;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiConversationController;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiController;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiSignedOriginTestSupport;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Provider("PjbLegalAiSignedOriginProvider")
@PactFolder("src/test/resources/pacts/provider")
class LegalAiSignedOriginProviderContractTest {

    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        var objectMapper = new ObjectMapper();
        var perimeterProperties = LegalAiSignedOriginTestSupport.perimeterProperties();
        var clientIpResolver = LegalAiSignedOriginTestSupport.clientIpResolver(perimeterProperties);
        stubResponses();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiController(surfaceFacadeService), new LegalAiConversationController(conversationService))
                .addFilters(
                        LegalAiSignedOriginTestSupport.apiRouteGovernanceFilter(clientIpResolver),
                        LegalAiSignedOriginTestSupport.requestBodyHashFilter(objectMapper),
                        LegalAiSignedOriginTestSupport.signedRequiredOriginGovernanceFilter(perimeterProperties, clientIpResolver, objectMapper, Duration.ofDays(3650))
                )
                .build();
        PactProviderSpring6Support.configure(context, mockMvc);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        request.with(servletRequest -> {
            servletRequest.setRemoteAddr(LegalAiSignedOriginTestSupport.TRUSTED_IP);
            return servletRequest;
        });
        context.verifyInteraction();
    }

    @State("legal ai edge accepts valid signed attestation")
    void legalAiEdgeAcceptsValidSignedAttestation() {
    }

    @State("legal ai edge rejects signed request without origin id")
    void legalAiEdgeRejectsSignedRequestWithoutOriginId() {
    }

    @State("legal ai edge rejects signed request with invalid signature")
    void legalAiEdgeRejectsSignedRequestWithInvalidSignature() {
    }

    @State("legal ai edge rejects signed request with mismatched body hash")
    void legalAiEdgeRejectsSignedRequestWithMismatchedBodyHash() {
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
