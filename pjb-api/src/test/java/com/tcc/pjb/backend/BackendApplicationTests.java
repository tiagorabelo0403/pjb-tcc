package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.configs.live.LiveClusterStateStore;
import com.tcc.pjb.backend.configs.live.NoOpLiveClusterStateStore;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
    classes = {BackendApplication.class, BackendApplicationTests.BootstrapBarrierTestConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.main.lazy-initialization=true",
        "spring.main.web-application-type=none",
        "spring.task.scheduling.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.cloud.refresh.enabled=false",
        "spring.data.jpa.repositories.bootstrap-mode=deferred",
        "spring.data.elasticsearch.repositories.enabled=false",
        "spring.data.redis.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "management.defaults.metrics.export.enabled=false",
        "management.simple.metrics.export.enabled=false",
        "management.health.defaults.enabled=false",
        "management.endpoints.enabled-by-default=false",
        "resilience4j.circuitbreaker.metrics.enabled=false",
        "resilience4j.retry.metrics.enabled=false",
        "resilience4j.bulkhead.metrics.enabled=false",
        "resilience4j.thread-pool-bulkhead.metrics.enabled=false",
        "resilience4j.timelimiter.metrics.enabled=false",
        "resilience4j.ratelimiter.metrics.enabled=false",
        "camunda.client.enabled=false",
        "camunda.client.zeebe.enabled=false",
        "zeebe.client.enabled=false",
        "pjb.ai.vector.mode=disabled",
        "pjb.cache.redis.enabled=false",
        "pjb.datasource.routing.enabled=false",
        "pjb.debug.enabled=false",
        "pjb.debug.jurisdiction.enabled=false",
        "pjb.gov.registry.enabled=false",
        "pjb.gov.vital-monitor.enabled=false",
        "pjb.integration.judicial.mni.enabled=false",
        "pjb.integration.judicial.mp.enabled=false",
        "pjb.integration.judicial.pdpj.enabled=false",
        "pjb.integration.judicial.pje.enabled=false",
        "pjb.integration.judicial.projudi.enabled=false",
        "pjb.integrations.pje.submission.mock-enabled=false",
        "pjb.jobs.dispatcher.enabled=false",
        "pjb.jobs.pg-listen.enabled=false",
        "pjb.kafka.enabled=false",
        "pjb.live.cluster.enabled=false",
        "pjb.scheduling.enabled=false",
        "pjb.search.enabled=false",
        "pjb.secretariat.enabled=false",
        "pjb.secretariat.ingest.enabled=false",
        "pjb.security.honeypot.enabled=false",
        "pjb.security.perimeter.origin-governance.enabled=false",
        "pjb.security.perimeter.blocklist.store=memory",
        "pjb.security.perimeter.ratelimit.store=memory",
        "pjb.sync.ibge.enabled=false"
    }
)
class BackendApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private LiveClusterStateStore liveClusterStateStore;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
        assertNotNull(environment);
        assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("test"));
        assertNotNull(liveClusterStateStore);
        assertFalse(liveClusterStateStore.distributed());
        assertFalse(environment.getProperty("pjb.kafka.enabled", Boolean.class, true));
        assertFalse(environment.getProperty("pjb.search.enabled", Boolean.class, true));
        assertFalse(environment.getProperty("pjb.secretariat.ingest.enabled", Boolean.class, true));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BootstrapBarrierTestConfiguration {

        @Bean
        @Primary
        LiveClusterStateStore bootstrapBarrierLiveClusterStateStore() {
            return new NoOpLiveClusterStateStore();
        }
    }
}
