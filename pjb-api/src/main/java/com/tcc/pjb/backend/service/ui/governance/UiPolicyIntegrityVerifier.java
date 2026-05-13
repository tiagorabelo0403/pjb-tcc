package com.tcc.pjb.backend.service.ui.governance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@Configuration
public class UiPolicyIntegrityVerifier {

    private static final Logger log = LoggerFactory.getLogger(UiPolicyIntegrityVerifier.class);

    @Bean
    public UiPolicyIntegrityState uiPolicyIntegrityState() {
        return new UiPolicyIntegrityState();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationRunner verifyUiPolicies(
            UiPolicyIntegrityProperties props,
            UiPolicyIntegrityState state,
            ResourceLoader loader,
            UiPolicySigningKeyProvider keyProvider,
            ObjectMapper mapper
    ) {
        Objects.requireNonNull(props, "props");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(keyProvider, "keyProvider");
        Objects.requireNonNull(mapper, "mapper");
        return args -> {
            boolean ok = true;
            ok &= verifyOne(props, loader, keyProvider, props.getAccessibilityPolicyLocation(), props.getAccessibilityPolicySignatureEnvName());
            ok &= verifyOne(props, loader, keyProvider, props.getAccessibilityAbacLocation(), props.getAccessibilityAbacSignatureEnvName());
            if (!ok) state.degrade();
            if (props.isEnforcePresentationBaseline()) {
                boolean b = verifyBaseline(loader, mapper, props.getPresentationBaselineLocation());
                if (!b) state.degrade();
            }
        };
    }

    private boolean verifyOne(
            UiPolicyIntegrityProperties props,
            ResourceLoader loader,
            UiPolicySigningKeyProvider keyProvider,
            String location,
            String envSigName
    ) {
        boolean requireSignature = props.isRequireSignature();
        try {
            Resource r = loader.getResource(location);
            if (!r.exists()) {
                if (requireSignature) {
                    log.error("UI policy missing: {}", location);
                    return false;
                }
                return true;
            }
            byte[] data = readAllBytes(r);
            byte[] expected = readExpectedSignature(props, loader, location, envSigName);
            if (expected == null) {
                if (requireSignature) {
                    log.error("UI policy signature missing: {}", location);
                    return false;
                }
                return true;
            }
            byte[] actual;
            try (UiPolicySigningKeyProvider.Handle h = keyProvider.acquire()) {
                actual = hmac(h.key(), data);
            }
            boolean eq = MessageDigest.isEqual(expected, actual);
            Arrays.fill(actual, (byte) 0);
            Arrays.fill(expected, (byte) 0);
            if (!eq) {
                log.error("UI policy signature mismatch: {}", location);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("UI policy integrity check failed: {}", location, e);
            return !requireSignature;
        }
    }

    private byte[] readExpectedSignature(
            UiPolicyIntegrityProperties props,
            ResourceLoader loader,
            String location,
            String envSigName
    ) throws Exception {
        return switch (props.getSignatureSource()) {
            case FILE -> {
                String sigLocation = location + ".sig";
                Resource sig = loader.getResource(sigLocation);
                if (!sig.exists()) yield null;
                byte[] raw = readAllBytes(sig);
                yield decodeSig(raw);
            }
            case ENV -> {
                String v = System.getenv(envSigName);
                if (v == null || v.isBlank()) yield null;
                byte[] out = Base64.getDecoder().decode(v.trim());
                yield Arrays.copyOf(out, out.length);
            }
        };
    }

    private boolean verifyBaseline(ResourceLoader loader, ObjectMapper mapper, String location) {
        try {
            Resource r = loader.getResource(location);
            if (!r.exists()) {
                log.error("UI presentation baseline missing: {}", location);
                return false;
            }
            byte[] bytes = readAllBytes(r);
            JsonNode root = mapper.readTree(bytes);
            if (root == null || !root.hasNonNull("version") || !root.has("hashes")) {
                log.error("UI presentation baseline invalid: {}", location);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("UI presentation baseline check failed: {}", location, e);
            return false;
        }
    }

    private static byte[] hmac(javax.crypto.SecretKey key, byte[] msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(msg);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decodeSig(byte[] bytes) {
        String s = new String(bytes, StandardCharsets.UTF_8).trim();
        byte[] out = Base64.getDecoder().decode(s);
        return Arrays.copyOf(out, out.length);
    }

    private static byte[] readAllBytes(Resource r) throws Exception {
        try (InputStream in = r.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
