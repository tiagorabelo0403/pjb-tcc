package com.tcc.pjb.backend.ai.juridica.api;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LegalAiEdgeGovernanceIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityPerimeterProperties perimeterProperties = new SecurityPerimeterProperties();
        perimeterProperties.setCorsAllowedOrigins(List.of("https://app.pjb.justica.br"));
        ClientIpResolver clientIpResolver = new ClientIpResolver(perimeterProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiController(surfaceFacadeService), new LegalAiConversationController(conversationService))
                .addFilters(apiRouteGovernanceFilter(clientIpResolver), apiRequestOriginGovernanceFilter(perimeterProperties, clientIpResolver))
                .build();
    }

    @Test
    void legalAiGovernedRoutesMustRejectUntrustedBrowserOrigin() throws Exception {
        String body = legalConversationRequestBody();

        mockMvc.perform(post("/api/ai/legal/minuta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://evil.example")
                        .content(legalDraftRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/browser_origin_not_allowed"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"));

        mockMvc.perform(post("/api/ai/legal/grounding/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://evil.example")
                        .content(legalGroundingRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/browser_origin_not_allowed"));

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://evil.example")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/browser_origin_not_allowed"));
    }

    @Test
    void legalAiGovernedRoutesMustRejectWrongContentTypeEvenWithTrustedOrigin() throws Exception {
        mockMvc.perform(post("/api/ai/legal/minuta")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Origin", "https://app.pjb.justica.br")
                        .content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/content_type_not_allowed"));

        mockMvc.perform(post("/api/ai/legal/grounding/check")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Origin", "https://app.pjb.justica.br")
                        .content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/content_type_not_allowed"));

        mockMvc.perform(post("/api/ai/legal/conversation")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Origin", "https://app.pjb.justica.br")
                        .content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/content_type_not_allowed"));
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
        return new ApiRequestOriginGovernanceFilter(properties, perimeterProperties, clientIpResolver, objectMapper);
    }

    private String legalDraftRequestBody() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("analiseV1", "Análise v1"),
                java.util.Map.entry("peticaoInicialText", "Texto inicial"),
                java.util.Map.entry("instrucoes", "Instruções"),
                java.util.Map.entry("objetivo", "Gerar minuta"),
                java.util.Map.entry("userProfile", "ADVOGADO"),
                java.util.Map.entry("processoId", "PROC-1"),
                java.util.Map.entry("ramo", "CIVEL"),
                java.util.Map.entry("rito", "COMUM"),
                java.util.Map.entry("classe", "OBRIGACAO_DE_FAZER"),
                java.util.Map.entry("attachments", java.util.List.of("documento.pdf")),
                java.util.Map.entry("contexto", java.util.Map.of("sourceSystem", "CNJ"))
        ));
    }

    private String legalGroundingRequestBody() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "texto", "Verificar consistência das citações do acórdão.",
                "ramo", "CIVEL",
                "rito", "COMUM",
                "classe", "OBRIGACAO_DE_FAZER",
                "citacoes", java.util.List.of("AgInt no REsp 1"),
                "contexto", java.util.Map.of("sourceSystem", "CNJ")
        ));
    }

    private String legalConversationRequestBody() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "conversationId", "conv-http",
                "processoId", "PROC-1",
                "message", "Preciso validar o cabimento recursal com base no acórdão anexado.",
                "userProfile", "ADVOGADO",
                "conversationHistory", java.util.List.of("Contexto anterior"),
                "attachments", java.util.List.of("acordao.pdf"),
                "contexto", java.util.Map.of("sourceSystem", "CNJ", "sigilo", "publico")
        ));
    }

    private static final class InMemoryRateLimiterStore implements RateLimiterStore {
        private final java.util.concurrent.ConcurrentHashMap<String, Long> values = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public long incr(String key, Duration ttl) {
            return values.merge(key, 1L, Long::sum);
        }
    }
}
