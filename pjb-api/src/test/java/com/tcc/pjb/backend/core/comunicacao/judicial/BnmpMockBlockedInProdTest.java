package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentValidator;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class BnmpMockBlockedInProdTest {

    private final MockGuardEnvironmentValidator validator = new MockGuardEnvironmentValidator();
    private final SpringApplication dummyApp = new SpringApplication();

    @Test
    void bnmpMockEnabledEmProd_bloqueiaStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.bnmp.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.bnmp.mock-enabled");
    }

    @Test
    void bnmpMockDisabledEmProd_permiteStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.hsm.mock-enabled", "false");
        env.setProperty("pjb.bnmp.mock-enabled", "false");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void bnmpMockEnabledEmTest_permiteStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("pjb.bnmp.mock-enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }
}
