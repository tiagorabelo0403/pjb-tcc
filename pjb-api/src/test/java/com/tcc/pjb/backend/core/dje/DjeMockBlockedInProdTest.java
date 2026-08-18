package com.tcc.pjb.backend.core.dje;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentValidator;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DjeMockBlockedInProdTest {

    private final MockGuardEnvironmentValidator validator = new MockGuardEnvironmentValidator();
    private final SpringApplication dummyApp = new SpringApplication();

    @Test
    void djeMockEnabledEmProd_bloqueiaStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.dje.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.dje.mock-enabled");
    }

    @Test
    void djeMockDisabledEmProd_permiteStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.dje.mock-enabled", "false");
        env.setProperty("pjb.hsm.mock-enabled", "false");
        env.setProperty("pjb.bnmp.mock-enabled", "false");
        env.setProperty("pjb.integrations.govbr.mock-enabled", "false");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void djeMockEnabledEmTest_permiteStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("pjb.dje.mock-enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void djeMockEnabledEmStaging_bloqueiaComProfileStaging() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");
        env.setProperty("pjb.dje.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("STAGING");
    }
}
