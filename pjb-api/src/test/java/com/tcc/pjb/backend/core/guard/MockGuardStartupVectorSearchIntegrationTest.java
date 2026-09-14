package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class MockGuardStartupVectorSearchIntegrationTest {

    private static final String INFRA_EXCLUSIONS =
            "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration";

    @Test
    void vectorModeMockEmProd_bloqueiaInicializacaoDoContexto() {
        SpringApplication app = new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .properties("spring.autoconfigure.exclude=" + INFRA_EXCLUSIONS)
                .application();

        assertThatThrownBy(() -> app.run(
                "--pjb.ai.vector.mode=mock",
                "--pjb.hsm.mock-enabled=false",
                "--pjb.bnmp.mock-enabled=false",
                "--pjb.icp.enabled=true",
                "--pjb.hsm.enabled=true"
        ))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.ai.vector.mode");
    }
}
