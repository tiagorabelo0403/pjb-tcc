package com.tcc.pjb.backend.service.ui.governance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@Configuration
public class UiPolicySigningKeyProviderConfig {

    @Bean
    public UiPolicySigningKeyProvider uiPolicySigningKeyProvider(UiPolicyIntegrityProperties props, KeyMaterialService keys) {
        Objects.requireNonNull(props, "props");
        Objects.requireNonNull(keys, "keys");
        return switch (props.getSigningKeySource()) {
            case KEY_MATERIAL -> new KeyMaterialProvider(keys);
            case ENV_EPHEMERAL -> new EnvEphemeralProvider(props.getSigningKeyEnvName());
        };
    }

    static final class KeyMaterialProvider implements UiPolicySigningKeyProvider {
        private final KeyMaterialService keys;
        KeyMaterialProvider(KeyMaterialService keys) { this.keys = keys; }
        @Override
        public Handle acquire() {
            SecretKey k = keys.getUiPolicySigningKey();
            return new Handle() {
                @Override
                public SecretKey key() { return k; }
                @Override
                public void close() { }
            };
        }
    }

    static final class EnvEphemeralProvider implements UiPolicySigningKeyProvider {
        private final String envName;
        EnvEphemeralProvider(String envName) { this.envName = envName; }
        @Override
        public Handle acquire() {
            String raw = System.getenv(envName);
            if (raw == null || raw.isBlank()) throw new IllegalStateException("missing env key: " + envName);
            byte[] decoded = Base64.getDecoder().decode(raw.trim());
            byte[] material = Arrays.copyOf(decoded, decoded.length);
            Arrays.fill(decoded, (byte) 0);
            SecretKey key = new SecretKeySpec(material, "HmacSHA256");
            return new Handle() {
                @Override
                public SecretKey key() { return key; }
                @Override
                public void close() { Arrays.fill(material, (byte) 0); }
            };
        }
    }
}
