package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class MockGuardProfileMultiProfilePrecedenceTest {

    private final MockGuardEnvironmentValidator validator = new MockGuardEnvironmentValidator();
    private final SpringApplication dummyApp = new SpringApplication();

    @Test
    void prodMaisAi_qualquerPerfilRealAtivaBloqueio() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod", "ai");
        env.setProperty("pjb.hsm.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class);
    }

    @Test
    void stagingMaisTest_stagingAtivaBloqueio() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging", "test");
        env.setProperty("pjb.bnmp.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("STAGING");
    }

    @Test
    void homologMaisIntegration_homologAtivaBloqueio() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("homolog", "integration");
        env.setProperty("pjb.hsm.mock-enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("HOMOLOG");
    }

    @Test
    void devMaisTestMaisLocal_nenhumRealAtiva_naoBloqueia() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev", "test", "local");
        env.setProperty("pjb.hsm.mock-enabled", "true");
        env.setProperty("pjb.bnmp.mock-enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void apenasTest_naoBloqueia() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("pjb.hsm.mock-enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void mockGuardProfile_isRealEnvironment_corretoPorEnum() {
        assertThat(MockGuardProfile.PROD.isRealEnvironment()).isTrue();
        assertThat(MockGuardProfile.STAGING.isRealEnvironment()).isTrue();
        assertThat(MockGuardProfile.HOMOLOG.isRealEnvironment()).isTrue();
        assertThat(MockGuardProfile.DEV.isRealEnvironment()).isFalse();
        assertThat(MockGuardProfile.TEST.isRealEnvironment()).isFalse();
        assertThat(MockGuardProfile.LOCAL.isRealEnvironment()).isFalse();
    }
}
