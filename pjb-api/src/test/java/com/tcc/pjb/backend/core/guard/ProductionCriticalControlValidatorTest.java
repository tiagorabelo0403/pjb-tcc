package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ProductionCriticalControlValidatorTest {

    private final ProductionCriticalControlValidator validator = new ProductionCriticalControlValidator();
    private final SpringApplication dummyApp = new SpringApplication();

    @Test
    void prodComIcpDesligado_deveLancarExcecao() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.icp.enabled", "false");
        env.setProperty("pjb.hsm.enabled", "true");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(ProductionCriticalControlViolationException.class)
                .hasMessageContaining("pjb.icp.enabled")
                .hasMessageContaining("ICP-Brasil");
    }

    @Test
    void prodComHsmDesligado_deveLancarExcecao() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.icp.enabled", "true");
        env.setProperty("pjb.hsm.enabled", "false");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(ProductionCriticalControlViolationException.class)
                .hasMessageContaining("pjb.hsm.enabled")
                .hasMessageContaining("HSM");
    }

    @Test
    void prodSemNenhumaPropriedade_deveLancarExcecaoParaAPrimeiraFaltante() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, dummyApp))
                .isInstanceOf(ProductionCriticalControlViolationException.class)
                .hasMessageContaining("pjb.icp.enabled");
    }

    @Test
    void prodComIcpEHsmLigados_naoDeveLancar() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("pjb.icp.enabled", "true");
        env.setProperty("pjb.hsm.enabled", "true");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void devComIcpEHsmDesligados_naoDeveLancar() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        env.setProperty("pjb.icp.enabled", "false");
        env.setProperty("pjb.hsm.enabled", "false");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }

    @Test
    void stagingComIcpEHsmDesligados_naoDeveLancar() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");
        env.setProperty("pjb.icp.enabled", "false");
        env.setProperty("pjb.hsm.enabled", "false");

        assertThatCode(() -> validator.postProcessEnvironment(env, dummyApp))
                .doesNotThrowAnyException();
    }
}
