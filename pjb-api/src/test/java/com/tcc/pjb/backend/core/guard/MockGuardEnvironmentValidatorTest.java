package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

class MockGuardEnvironmentValidatorTest {

    private final MockGuardEnvironmentValidator validator = new MockGuardEnvironmentValidator();
    private final SpringApplication dummyApp = new SpringApplication();

    @Test
    void prodComHsmMockEnabled_deveLancarExcecao() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.hsm.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.hsm.mock-enabled")
                .hasMessageContaining("PROD");
    }

    @Test
    void prodComBnmpMockEnabled_deveLancarExcecao() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.bnmp.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.bnmp.mock-enabled")
                .hasMessageContaining("PROD");
    }

    @Test
    void stagingComHsmMockEnabled_deveLancarExcecao() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");
        env.setProperty("pjb.hsm.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("STAGING");
    }

    @Test
    void testComHsmMockEnabled_naoDeveLancar() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("pjb.hsm.mock-enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void devComMockEnabled_naoDeveLancar() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        env.setProperty("pjb.hsm.mock-enabled", "true");
        env.setProperty("pjb.bnmp.mock-enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void prodSemMock_gravaRealEnvironmentProperty() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.hsm.mock-enabled", "false");
        env.setProperty("pjb.bnmp.mock-enabled", "false");

        validator.postProcessEnvironment(env, dummyApp);

        assertThatCode(() -> {
            String val = env.getProperty(MockGuardEnvironmentValidator.REAL_ENV_PROPERTY);
            assert "true".equals(val) : "esperado true, obtido: " + val;
        }).doesNotThrowAnyException();
    }
}
