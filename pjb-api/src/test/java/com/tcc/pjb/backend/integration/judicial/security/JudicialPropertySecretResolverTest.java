package com.tcc.pjb.backend.integration.judicial.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

class JudicialPropertySecretResolverTest {

    @Test
    void resolvesLiteralBase64EnvironmentAndSystemSecrets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("PJB_SECRET_ALIAS", "A3-TJCE");
        System.setProperty("pjb.secret.system", "TRUST-ANCHOR");
        JudicialPropertySecretResolver resolver = new JudicialPropertySecretResolver(environment, new DefaultResourceLoader());

        assertThat(resolver.resolve("literal:secret-value")).isEqualTo("secret-value");
        assertThat(resolver.resolve("base64:" + Base64.getEncoder().encodeToString("crypto-pin".getBytes(StandardCharsets.UTF_8)))).isEqualTo("crypto-pin");
        assertThat(resolver.resolve("env:PJB_SECRET_ALIAS")).isEqualTo("A3-TJCE");
        assertThat(resolver.resolve("sys:pjb.secret.system")).isEqualTo("TRUST-ANCHOR");
    }
}
