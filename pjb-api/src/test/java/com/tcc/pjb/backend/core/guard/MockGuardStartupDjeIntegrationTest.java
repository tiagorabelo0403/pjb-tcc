package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class MockGuardStartupDjeIntegrationTest {

    private static final String INFRA_EXCLUSIONS =
            "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration";

    /**
     * Cenário A: mock-enabled=true em prod → EPP dispara MockGuardViolationException antes de qualquer bean.
     */
    @Test
    void djeMockEnabledEmProd_bloqueiaInicializacaoDoContexto() {
        SpringApplication app = new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .properties("spring.autoconfigure.exclude=" + INFRA_EXCLUSIONS)
                .application();

        assertThatThrownBy(() -> app.run(
                "--pjb.dje.mock-enabled=true",
                "--pjb.hsm.mock-enabled=false",
                "--pjb.bnmp.mock-enabled=false",
                "--pjb.integrations.govbr.mock-enabled=false"
        ))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.dje.mock-enabled");
    }

    /**
     * Cenário B (AJUSTE 2): mock-enabled=false + enabled=false em ambiente onde o barrier DJE
     * está ativo (prod) → DjeConfiguration é carregado mas nenhum bean DjeHttpClient é
     * registrado → falha ao inicializar o contexto.
     *
     * Usa profile=prod com infra excluída. O EPP não dispara (mock=false), mas DjePublicacaoService
     * depende de DjeHttpClient que não existe → NoSuchBeanDefinitionException em algum ponto da
     * cadeia de inicialização.
     * Documenta o fail-fast intencional: sem mock e sem implementação real, o contexto não sobe.
     */
    @Test
    void djeMockDisabledMaisEnabledFalse_falhaAoInicializarContexto() {
        SpringApplication app = new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .properties("spring.autoconfigure.exclude=" + INFRA_EXCLUSIONS)
                .application();

        assertThatThrownBy(() -> app.run(
                "--pjb.dje.mock-enabled=false",
                "--pjb.dje.enabled=false",
                "--pjb.hsm.mock-enabled=false",
                "--pjb.bnmp.mock-enabled=false",
                "--pjb.integrations.govbr.mock-enabled=false"
        ))
                .satisfies(t -> {
                    Throwable cause = t;
                    boolean foundNoSuchBean = false;
                    while (cause != null) {
                        if (cause instanceof NoSuchBeanDefinitionException) {
                            foundNoSuchBean = true;
                            break;
                        }
                        cause = cause.getCause();
                    }
                    org.assertj.core.api.Assertions.assertThat(foundNoSuchBean)
                            .as("Esperado NoSuchBeanDefinitionException na cadeia de causa — fail-fast de DjeHttpClient ausente")
                            .isTrue();
                });
    }
}
