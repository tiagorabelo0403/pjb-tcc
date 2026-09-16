package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class MockGuardStartupVectorSearchIntegrationTest {

    private static final String INFRA_EXCLUSIONS =
            "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration," +
            "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration," +
            "org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration," +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration";

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
