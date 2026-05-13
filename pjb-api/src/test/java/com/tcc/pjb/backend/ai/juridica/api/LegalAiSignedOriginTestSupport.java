package com.tcc.pjb.backend.ai.juridica.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceFilter;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceProperties;
import com.tcc.pjb.backend.configs.security.perimeter.ApiRequestOriginGovernanceFilter;
import com.tcc.pjb.backend.configs.security.perimeter.ApiRequestOriginGovernanceProperties;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.configs.security.perimeter.SecurityPerimeterProperties;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.core.security.device.reqhash.BodyHashService;
import com.tcc.pjb.backend.core.security.device.reqhash.RequestBodyHashFilter;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterStore;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class LegalAiSignedOriginTestSupport {

    public static final String TRUSTED_ORIGIN_ID = "edge-app";
    public static final String TRUSTED_SECRET = "super-secret";
    public static final String TRUSTED_IP = "10.10.10.10";

    private LegalAiSignedOriginTestSupport() {
    }

    public static SecurityPerimeterProperties perimeterProperties() {
        SecurityPerimeterProperties properties = new SecurityPerimeterProperties();
        properties.setCorsAllowedOrigins(List.of("https://app.pjb.justica.br"));
        return properties;
    }

    public static ClientIpResolver clientIpResolver(SecurityPerimeterProperties perimeterProperties) {
        return new ClientIpResolver(perimeterProperties);
    }

    public static RequestBodyHashFilter requestBodyHashFilter(ObjectMapper objectMapper) {
        DeviceSecurityProperties properties = new DeviceSecurityProperties();
        properties.setBodyHashMaxBytes(262144);
        return new RequestBodyHashFilter(properties, new BodyHashService(objectMapper), objectMapper);
    }

    public static ApiRouteGovernanceFilter apiRouteGovernanceFilter(ClientIpResolver clientIpResolver) {
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
        return new ApiRouteGovernanceFilter(properties, clientIpResolver, new InMemoryRateLimiterStore(), new JudicialScaleProfileResolver());
    }

    public static ApiRequestOriginGovernanceFilter signedRequiredOriginGovernanceFilter(SecurityPerimeterProperties perimeterProperties,
                                                                                        ClientIpResolver clientIpResolver,
                                                                                        ObjectMapper objectMapper) {
        return signedRequiredOriginGovernanceFilter(perimeterProperties, clientIpResolver, objectMapper, Duration.ofMinutes(5));
    }

    public static ApiRequestOriginGovernanceFilter signedRequiredOriginGovernanceFilter(SecurityPerimeterProperties perimeterProperties,
                                                                                        ClientIpResolver clientIpResolver,
                                                                                        ObjectMapper objectMapper,
                                                                                        Duration maxTimestampSkew) {
        ApiRequestOriginGovernanceProperties properties = new ApiRequestOriginGovernanceProperties();
        properties.setEnabled(true);
        properties.setMaxTimestampSkew(Duration.ofDays(3650));
        properties.setMaxTimestampSkew(maxTimestampSkew);
        properties.getGovernedPrefixes().clear();
        properties.getGovernedPrefixes().add("/api/ai/legal/");
        properties.getSignedRequiredPrefixes().clear();
        properties.getSignedRequiredPrefixes().add("/api/ai/legal/");
        ApiRequestOriginGovernanceProperties.TrustedOrigin trustedOrigin = new ApiRequestOriginGovernanceProperties.TrustedOrigin();
        trustedOrigin.setActive(true);
        trustedOrigin.setId(TRUSTED_ORIGIN_ID);
        trustedOrigin.setSecret(TRUSTED_SECRET);
        trustedOrigin.setAllowedCidrs(List.of("10.0.0.0/8"));
        trustedOrigin.setAllowedPathPrefixes(List.of("/api/ai/legal/"));
        trustedOrigin.setAllowedMethods(List.of("POST"));
        trustedOrigin.setAllowedOrigins(List.of());
        properties.getTrustedOrigins().add(trustedOrigin);
        return new ApiRequestOriginGovernanceFilter(properties, perimeterProperties, clientIpResolver, objectMapper);
    }


    public static ApiRequestOriginGovernanceFilter selectiveConversationOriginGovernanceFilter(SecurityPerimeterProperties perimeterProperties,
                                                                                               ClientIpResolver clientIpResolver,
                                                                                               ObjectMapper objectMapper) {
        ApiRequestOriginGovernanceProperties properties = new ApiRequestOriginGovernanceProperties();
        properties.setEnabled(true);
        properties.setMaxTimestampSkew(Duration.ofDays(3650));
        properties.getGovernedPrefixes().clear();
        properties.getGovernedPrefixes().add("/api/ai/legal/");
        properties.getTrustedBrowserOrigins().add("https://app.pjb.justica.br");

        ApiRequestOriginGovernanceProperties.SelectiveSignedRule conversationRule = new ApiRequestOriginGovernanceProperties.SelectiveSignedRule();
        conversationRule.setName("legal-ai-conversation-sensitive-capabilities");
        conversationRule.setPaths(List.of("/api/ai/legal/conversation"));
        conversationRule.setCapabilityValues(List.of("LEGAL_DRAFT_V2", "LEGAL_GROUNDING_CHECK_V3", "LEGAL_CAPABILITY_RECOVERY_V1"));
        conversationRule.setCapabilityJsonPointers(List.of("/context/requestedCapability", "/context/capability", "/capability"));
        properties.getSelectiveSignedRules().add(conversationRule);

        ApiRequestOriginGovernanceProperties.TrustedOrigin trustedOrigin = new ApiRequestOriginGovernanceProperties.TrustedOrigin();
        trustedOrigin.setActive(true);
        trustedOrigin.setId(TRUSTED_ORIGIN_ID);
        trustedOrigin.setSecret(TRUSTED_SECRET);
        trustedOrigin.setAllowedCidrs(List.of("10.0.0.0/8"));
        trustedOrigin.setAllowedPathPrefixes(List.of("/api/ai/legal/"));
        trustedOrigin.setAllowedMethods(List.of("POST"));
        trustedOrigin.setAllowedOrigins(List.of("https://app.pjb.justica.br"));
        properties.getTrustedOrigins().add(trustedOrigin);
        return new ApiRequestOriginGovernanceFilter(properties, perimeterProperties, clientIpResolver, objectMapper);
    }

    public static SignedRequest signedRequest(String path, String body) {
        return signedRequest(path, Instant.now(), canonicalJsonBodyHash(body), TRUSTED_ORIGIN_ID, TRUSTED_SECRET);
    }

    public static SignedRequest signedRequest(String path,
                                              Instant timestamp,
                                              String bodyHash,
                                              String originId,
                                              String secret) {
        try {
            String canonical = canonicalMaterial(originId, timestamp.toString(), "POST", path, bodyHash);
            String signature = sign(secret, canonical);
            return new SignedRequest(originId, timestamp.toString(), bodyHash, signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao montar request assinado de teste.", ex);
        }
    }

    public static String legalDraftRequestBody(ObjectMapper objectMapper) throws Exception {
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("analiseV1", "Análise v1"),
                Map.entry("peticaoInicialText", "Texto inicial"),
                Map.entry("instrucoes", "Instruções"),
                Map.entry("objetivo", "Gerar minuta"),
                Map.entry("userProfile", "ADVOGADO"),
                Map.entry("processoId", "PROC-1"),
                Map.entry("ramo", "CIVEL"),
                Map.entry("rito", "COMUM"),
                Map.entry("classe", "OBRIGACAO_DE_FAZER"),
                Map.entry("attachments", List.of("documento.pdf")),
                Map.entry("contexto", Map.of("sourceSystem", "CNJ"))
        ));
    }

    public static String legalGroundingRequestBody(ObjectMapper objectMapper) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "texto", "Verificar consistência das citações do acórdão.",
                "ramo", "CIVEL",
                "rito", "COMUM",
                "classe", "OBRIGACAO_DE_FAZER",
                "citacoes", List.of("AgInt no REsp 1"),
                "contexto", Map.of("sourceSystem", "CNJ")
        ));
    }

    public static String legalConversationRequestBody(ObjectMapper objectMapper) throws Exception {
        return legalConversationRequestBody(objectMapper, "LEGAL_GENERAL_ASSIST_V3");
    }

    public static String legalConversationRequestBody(ObjectMapper objectMapper, String capability) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "conversationId", "conv-http",
                "processoId", "PROC-1",
                "message", "Preciso validar o cabimento recursal com base no acórdão anexado.",
                "userProfile", "ADVOGADO",
                "history", List.of("Contexto anterior"),
                "attachments", List.of("acordao.pdf"),
                "context", Map.of("sourceSystem", "CNJ", "sigilo", "publico", "requestedCapability", capability)
        ));
    }

    private static String canonicalMaterial(String originId, String timestamp, String method, String path, String bodyHash) {
        return originId + '\n' + timestamp + '\n' + method + '\n' + path + '\n' + bodyHash;
    }

    private static String sign(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String canonicalJsonBodyHash(String body) {
        return new BodyHashService(new ObjectMapper()).canonicalJsonHash(body.getBytes(StandardCharsets.UTF_8));
    }

    public record SignedRequest(String originId, String timestamp, String bodyHash, String signature) {
    }

    private static final class InMemoryRateLimiterStore implements RateLimiterStore {
        private final java.util.concurrent.ConcurrentHashMap<String, Long> values = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public long incr(String key, Duration ttl) {
            return values.merge(key, 1L, Long::sum);
        }
    }
}
