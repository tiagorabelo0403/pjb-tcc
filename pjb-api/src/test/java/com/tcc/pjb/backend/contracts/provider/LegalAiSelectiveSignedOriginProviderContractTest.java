package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiConversationController;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiController;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiSignedOriginTestSupport;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Provider("PjbLegalAiSelectiveSignedOriginProvider")
@PactFolder("src/test/resources/pacts/provider")
class LegalAiSelectiveSignedOriginProviderContractTest {

    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        var objectMapper = new ObjectMapper();
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
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiController(surfaceFacadeService), new LegalAiConversationController(conversationService))
                .addFilters(
                        LegalAiSignedOriginTestSupport.apiRouteGovernanceFilter(clientIpResolver),
                        LegalAiSignedOriginTestSupport.requestBodyHashFilter(objectMapper),
                        LegalAiSignedOriginTestSupport.selectiveConversationOriginGovernanceFilter(perimeterProperties, clientIpResolver, objectMapper)
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

    @State("legal ai conversation accepts trusted browser for non sensitive capability")
    void legalAiConversationAcceptsTrustedBrowserForNonSensitiveCapability() {
    }

    @State("legal ai conversation requires signed attestation for sensitive capability")
    void legalAiConversationRequiresSignedAttestationForSensitiveCapability() {
    }

    @State("legal ai conversation accepts signed attestation for sensitive capability")
    void legalAiConversationAcceptsSignedAttestationForSensitiveCapability() {
    }
}
