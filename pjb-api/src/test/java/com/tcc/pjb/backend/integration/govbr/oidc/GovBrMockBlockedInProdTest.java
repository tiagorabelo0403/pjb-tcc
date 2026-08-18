package com.tcc.pjb.backend.integration.govbr.oidc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentValidator;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class GovBrMockBlockedInProdTest {

    private final MockGuardEnvironmentValidator validator = new MockGuardEnvironmentValidator();
    private final SpringApplication dummyApp = new SpringApplication();

    @Test
    void govBrMockEnabledEmProd_bloqueiaStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.integrations.govbr.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.integrations.govbr.mock-enabled");
    }

    @Test
    void govBrMockDisabledEmProd_permiteStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.integrations.govbr.mock-enabled", "false");
        env.setProperty("pjb.hsm.mock-enabled", "false");
        env.setProperty("pjb.bnmp.mock-enabled", "false");
        env.setProperty("pjb.dje.mock-enabled", "false");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void govBrMockEnabledEmTest_permiteStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("pjb.integrations.govbr.mock-enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }
}
