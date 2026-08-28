package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.BackendApplication;
import com.tcc.pjb.backend.core.dje.DjeConfiguration;
import com.tcc.pjb.backend.core.dje.DjeHttpClient;
import com.tcc.pjb.backend.core.dje.DjePartesNotificacaoPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

class MockGuardStartupDjeIntegrationTest {

    private static final String INFRA_EXCLUSIONS =
            "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration";

    /**
     * Cenário A: mock-enabled=true em prod → EPP dispara MockGuardViolationException antes de
     * qualquer bean ser criado (EnvironmentPostProcessor roda antes do grafo de beans), por isso
     * é seguro subir o BackendApplication inteiro aqui — não depende de ordem de inicialização.
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
                "--pjb.integrations.govbr.mock-enabled=false",
                "--pjb.icp.enabled=true",
                "--pjb.hsm.enabled=true"
        ))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.dje.mock-enabled");
    }

    /**
     * Cenário B: mock-enabled=false + enabled=false em ambiente real → nenhum bean DjeHttpClient
     * (nem DjePartesNotificacaoPort) é registrado. Isolado via ApplicationContextRunner carregando
     * só DjeConfiguration — subir o BackendApplication inteiro aqui é frágil, pois depende da
     * ordem incidental em que o Spring cria beans não relacionados que também exigem infra
     * (Kafka/Elasticsearch/DataSource) excluída deste teste, e qualquer um deles pode "vencer"
     * a corrida e mascarar o que este teste realmente verifica.
     */
    @Test
    void realEnvironmentSemMockESemImplementacaoReal_naoRegistraBeanDjeHttpClient() {
        contextRunner()
                .withPropertyValues(
                        "pjb.dje.mock-enabled=false",
                        "pjb.dje.enabled=false",
                        "pjb.mock-guard.real-environment=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DjeHttpClient.class);
                    assertThat(context).doesNotHaveBean(DjePartesNotificacaoPort.class);
                });
    }

    /**
     * Fora de ambiente real (dev/test/demo), o mesmo cenário registra um bean de fallback que
     * mantém o contexto de pé — e só falha se alguém realmente tentar enviar ao DJE.
     */
    @Test
    void ambienteNaoRealSemMock_registraFallbackQueFalhaSoAoUsar() {
        contextRunner()
                .withPropertyValues(
                        "pjb.dje.mock-enabled=false",
                        "pjb.dje.enabled=false",
                        "pjb.mock-guard.real-environment=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DjeHttpClient.class);
                    assertThat(context).hasSingleBean(DjePartesNotificacaoPort.class);

                    DjeHttpClient client = context.getBean(DjeHttpClient.class);
                    assertThatThrownBy(() -> client.enviar("TJSP", "<ato/>", "DESPACHO"))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("dje_integracao_nao_configurada");
                });
    }

    private static ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(GuardSupportConfig.class, DjeConfiguration.class);
    }

    @Configuration
    static class GuardSupportConfig {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        MockGuardEnvironmentQuery mockGuardEnvironmentQuery(Environment environment, MeterRegistry meterRegistry) {
            return new MockGuardEnvironmentQuery(environment, meterRegistry);
        }
    }
}
