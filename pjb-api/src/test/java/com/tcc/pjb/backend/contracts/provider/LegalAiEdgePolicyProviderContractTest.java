package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.Mockito.mock;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.State;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiConversationController;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiController;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceFilter;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceProperties;
import com.tcc.pjb.backend.configs.security.perimeter.ApiRequestOriginGovernanceFilter;
import com.tcc.pjb.backend.configs.security.perimeter.ApiRequestOriginGovernanceProperties;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.configs.security.perimeter.SecurityPerimeterProperties;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterStore;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Provider("PjbLegalAiEdgePolicyProvider")
@PactFolder("src/test/resources/pacts/provider")
class LegalAiEdgePolicyProviderContractTest {

    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        SecurityPerimeterProperties perimeterProperties = new SecurityPerimeterProperties();
        perimeterProperties.setCorsAllowedOrigins(List.of("https://app.pjb.justica.br"));
        ClientIpResolver clientIpResolver = new ClientIpResolver(perimeterProperties);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiController(surfaceFacadeService), new LegalAiConversationController(conversationService))
                .addFilters(apiRouteGovernanceFilter(clientIpResolver), apiRequestOriginGovernanceFilter(perimeterProperties, clientIpResolver))
                .build();
        PactProviderSpring6Support.configure(context, mockMvc);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("legal ai edge rejects untrusted browser origin")
    void legalAiEdgeRejectsUntrustedBrowserOrigin() {
    }

    @State("legal ai edge rejects unsupported content type")
    void legalAiEdgeRejectsUnsupportedContentType() {
    }

    private ApiRouteGovernanceFilter apiRouteGovernanceFilter(ClientIpResolver clientIpResolver) {
        ApiRouteGovernanceProperties properties = new ApiRouteGovernanceProperties();
        ApiRouteGovernanceProperties.Rule rule = new ApiRouteGovernanceProperties.Rule();
        rule.setName("legal-ai-governed-surfaces");
        rule.setPaths(List.of("/api/ai/legal/minuta", "/api/ai/legal/grounding/check", "/api/ai/legal/conversation"));
        rule.setMethods(List.of("POST"));
        rule.setAllowedContentTypes(List.of("application/json"));
        rule.setMaxRequestsPerWindow(120L);
        rule.setRateWindowSeconds(60);
        rule.setRateLimitKeyStrategy("ip_user");
        rule.setNoStoreResponse(true);
        properties.getRules().add(rule);
        RateLimiterStore rateLimiterStore = new InMemoryRateLimiterStore();
        return new ApiRouteGovernanceFilter(properties, clientIpResolver, rateLimiterStore, new JudicialScaleProfileResolver());
    }

    private ApiRequestOriginGovernanceFilter apiRequestOriginGovernanceFilter(SecurityPerimeterProperties perimeterProperties,
                                                                              ClientIpResolver clientIpResolver) {
        ApiRequestOriginGovernanceProperties properties = new ApiRequestOriginGovernanceProperties();
        properties.setEnabled(true);
        properties.getGovernedPrefixes().clear();
        properties.getGovernedPrefixes().add("/api/ai/legal/");
        properties.getTrustedBrowserOrigins().add("https://app.pjb.justica.br");
        return new ApiRequestOriginGovernanceFilter(properties, perimeterProperties, clientIpResolver, new ObjectMapper());
    }

    private static final class InMemoryRateLimiterStore implements RateLimiterStore {
        private final java.util.concurrent.ConcurrentHashMap<String, Long> values = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public long incr(String key, Duration ttl) {
            return values.merge(key, 1L, Long::sum);
        }
    }
}
