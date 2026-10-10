package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

class EmissorJwtConfiguradoTest {

    @Test
    void semEmissorConfiguradoOResourceServerNaoTemQuemValidar() {
        assertThat(EmissorJwtConfigurado.presente(new MockEnvironment())).isFalse();
        assertThat(EmissorJwtConfigurado.presente(new MockEnvironment()
                .withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", " "))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.security.oauth2.resourceserver.jwt.issuer-uri",
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            "spring.security.oauth2.resourceserver.jwt.public-key-location"})
    void qualquerFonteDeChaveDoEmissorLigaOResourceServer(String propriedade) {
        assertThat(EmissorJwtConfigurado.presente(new MockEnvironment()
                .withProperty(propriedade, "https://emissor.exemplo.gov.br"))).isTrue();
    }

    @Test
    void emissorInformadoPorVariavelDeAmbienteTambemLigaOResourceServer() {
        StandardEnvironment ambiente = new StandardEnvironment();
        ambiente.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI", "https://emissor.exemplo.gov.br")));
        ConfigurationPropertySources.attach(ambiente);

        assertThat(EmissorJwtConfigurado.presente(ambiente)).isTrue();
    }
}
