package com.tcc.pjb.backend.configs.security.perimeter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRequestOriginGovernanceFilterTest {

    @Test
    void shouldAllowGovernedMutatingRequestWhenBrowserOriginIsTrusted() throws Exception {
        SecurityPerimeterProperties perimeter = new SecurityPerimeterProperties();
        perimeter.getCorsAllowedOrigins().add("https://app.pjb.justica.br");

        ApiRequestOriginGovernanceProperties properties = new ApiRequestOriginGovernanceProperties();
        properties.setEnabled(true);

        ApiRequestOriginGovernanceFilter filter = new ApiRequestOriginGovernanceFilter(
                properties,
                perimeter,
                new ClientIpResolver(perimeter),
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/processual/peticoes");
        request.addHeader("Origin", "https://app.pjb.justica.br");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setHeader("chain", "ok"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("chain")).isEqualTo("ok");
        assertThat(response.getHeader("X-PJB-Origin-Mode")).isEqualTo("BROWSER_ALLOWLIST");
        assertThat(response.getHeader("X-PJB-Origin-Subject")).isEqualTo("https://app.pjb.justica.br");
    }

    @Test
    void shouldRejectGovernedMutatingRequestWithoutKnownOriginOrSignature() throws Exception {
        SecurityPerimeterProperties perimeter = new SecurityPerimeterProperties();
        ApiRequestOriginGovernanceProperties properties = new ApiRequestOriginGovernanceProperties();
        properties.setEnabled(true);

        ApiRequestOriginGovernanceFilter filter = new ApiRequestOriginGovernanceFilter(
                properties,
                perimeter,
                new ClientIpResolver(perimeter),
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/legal/minuta");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain should not be invoked");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("origin_attestation_required");
    }

    @Test
    void shouldAllowGovernedMutatingRequestWhenSignedOriginIsTrusted() throws Exception {
        SecurityPerimeterProperties perimeter = new SecurityPerimeterProperties();
        ApiRequestOriginGovernanceProperties properties = new ApiRequestOriginGovernanceProperties();
        properties.setEnabled(true);
        ApiRequestOriginGovernanceProperties.TrustedOrigin trustedOrigin = new ApiRequestOriginGovernanceProperties.TrustedOrigin();
        trustedOrigin.setActive(true);
        trustedOrigin.setId("edge-app");
        trustedOrigin.setSecret("super-secret");
        trustedOrigin.setAllowedCidrs(java.util.List.of("10.0.0.0/8"));
        trustedOrigin.setAllowedPathPrefixes(java.util.List.of("/api/ai/", "/api/v1/processual/"));
        trustedOrigin.setAllowedMethods(java.util.List.of("POST", "PUT", "PATCH", "DELETE"));
        properties.getTrustedOrigins().add(trustedOrigin);

        ApiRequestOriginGovernanceFilter filter = new ApiRequestOriginGovernanceFilter(
                properties,
                perimeter,
                new ClientIpResolver(perimeter),
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/legal/minuta");
        request.setRemoteAddr("10.10.10.10");
        request.setContentType("application/json");
        request.setContent("{\"pedido\":\"teste\"}".getBytes(StandardCharsets.UTF_8));
        String bodyHash = sha256Hex("{\"pedido\":\"teste\"}".getBytes(StandardCharsets.UTF_8));
        request.setAttribute("PJB_BODY_HASH", bodyHash);
        String timestamp = Instant.now().toString();
        request.addHeader("X-PJB-Origin-Id", "edge-app");
        request.addHeader("X-PJB-Timestamp", timestamp);
        request.addHeader("X-PJB-Body-Hash", bodyHash);
        request.addHeader("X-PJB-Signature-Alg", "HMAC-SHA256");
        request.addHeader("X-PJB-Signature", sign("super-secret", canonicalMaterial("edge-app", timestamp, "POST", "/api/ai/legal/minuta", bodyHash)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setHeader("chain", "ok"));

        assertThat(response.getHeader("chain")).isEqualTo("ok");
        assertThat(response.getHeader("X-PJB-Origin-Mode")).isEqualTo("SIGNED_ATTESTATION");
        assertThat(response.getHeader("X-PJB-Origin-Subject")).isEqualTo("edge-app");
    }

    @Test
    void shouldRejectSignedOriginWhenBodyHashDoesNotMatchCanonicalHash() throws Exception {
        SecurityPerimeterProperties perimeter = new SecurityPerimeterProperties();
        ApiRequestOriginGovernanceProperties properties = new ApiRequestOriginGovernanceProperties();
        properties.setEnabled(true);
        ApiRequestOriginGovernanceProperties.TrustedOrigin trustedOrigin = new ApiRequestOriginGovernanceProperties.TrustedOrigin();
        trustedOrigin.setActive(true);
        trustedOrigin.setId("edge-app");
        trustedOrigin.setSecret("super-secret");
        trustedOrigin.setAllowedCidrs(java.util.List.of("10.0.0.0/8"));
        trustedOrigin.setAllowedPathPrefixes(java.util.List.of("/api/ai/"));
        trustedOrigin.setAllowedMethods(java.util.List.of("POST"));
        properties.getTrustedOrigins().add(trustedOrigin);

        ApiRequestOriginGovernanceFilter filter = new ApiRequestOriginGovernanceFilter(
                properties,
                perimeter,
                new ClientIpResolver(perimeter),
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/legal/minuta");
        request.setRemoteAddr("10.10.10.10");
        request.setContentType("application/json");
        request.setContent("{\"pedido\":\"teste\"}".getBytes(StandardCharsets.UTF_8));
        request.setAttribute("PJB_BODY_HASH", sha256Hex("{\"pedido\":\"teste\"}".getBytes(StandardCharsets.UTF_8)));
        String badBodyHash = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String timestamp = Instant.now().toString();
        request.addHeader("X-PJB-Origin-Id", "edge-app");
        request.addHeader("X-PJB-Timestamp", timestamp);
        request.addHeader("X-PJB-Body-Hash", badBodyHash);
        request.addHeader("X-PJB-Signature", sign("super-secret", canonicalMaterial("edge-app", timestamp, "POST", "/api/ai/legal/minuta", badBodyHash)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain should not be invoked");
        });

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("signed_origin_body_hash_mismatch");
    }

    private static String canonicalMaterial(String originId, String timestamp, String method, String path, String bodyHash) {
        return originId + '\n' + timestamp + '\n' + method + '\n' + path + '\n' + bodyHash;
    }

    private static String sign(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String sha256Hex(byte[] material) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(material));
    }
}
