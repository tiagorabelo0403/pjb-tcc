package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.configs.live.LiveClusterStateStore;
import com.tcc.pjb.backend.configs.live.NoOpLiveClusterStateStore;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCompetenceRuleStateRepository;
import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoPericiaEvidenceIntelligenceService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaPublicationGateService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaSecurityPipelineService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaStorageShieldService;
import com.tcc.pjb.backend.service.upload.UploadContentPolicyService;
import java.lang.reflect.Field;
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
import org.springframework.test.util.AopTestUtils;

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
        "spring.autoconfigure.exclude=org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchRepositoriesAutoConfiguration,org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchReactiveRepositoriesAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration,org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
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
        "pjb.sync.ibge.enabled=false",
        "pjb.storage.upload.max-document-bytes=12345678"
    }
)
class BackendApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private LiveClusterStateStore liveClusterStateStore;

    @Autowired
    private InstitutionalCompetenceRuleStateRepository institutionalCompetenceRuleStateRepository;

    @Autowired
    private InstitutionalCatalogGovernanceStateRepository institutionalCatalogGovernanceStateRepository;

    @Autowired
    private UploadContentPolicyService uploadContentPolicyService;

    @Autowired
    private PeticionamentoPericiaEvidenceIntelligenceService peticionamentoPericiaEvidenceIntelligenceService;

    @Autowired
    private PeticionamentoMediaPublicationGateService peticionamentoMediaPublicationGateService;

    @Autowired
    private PeticionamentoMediaSecurityPipelineService peticionamentoMediaSecurityPipelineService;

    @Autowired
    private PeticionamentoMediaStorageShieldService peticionamentoMediaStorageShieldService;

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

    /**
     * D-inject-anotacao-no-construtor-errado: um commit anterior (0c7e926a) anotou @Inject no
     * construtor DEGRADADO (sem args, zerando dependencias reais) em vez do construtor real destas
     * 2 classes — o Spring escolhia sempre o degradado, deixando a persistencia JPA de
     * InstitutionalCompetenceRuleSnapshot/InstitutionalCatalogGovernanceSnapshot inatingivel em
     * producao (tudo caia no fallback ConcurrentHashMap, que nao sobrevive a restart). Corrigido
     * movendo @Inject para o construtor com stateStore/codec/jpaRepository reais. Este teste prova
     * com reflection que o contexto real do Spring instancia via o construtor completo (jpaRepository
     * != null), nao mais via o degradado.
     */
    @Test
    void institutionalStateRepositoriesUsamConstrutorRealComJpaRepositoryWireado() throws Exception {
        assertNotNull(readPrivateField(institutionalCompetenceRuleStateRepository, "jpaRepository"),
                "InstitutionalCompetenceRuleStateRepository.jpaRepository deveria vir do construtor real (Spring), nao do degradado");
        assertNotNull(readPrivateField(institutionalCompetenceRuleStateRepository, "stateStore"),
                "InstitutionalCompetenceRuleStateRepository.stateStore deveria vir do construtor real (Spring), nao do degradado");
        assertNotNull(readPrivateField(institutionalCatalogGovernanceStateRepository, "jpaRepository"),
                "InstitutionalCatalogGovernanceStateRepository.jpaRepository deveria vir do construtor real (Spring), nao do degradado");
        assertNotNull(readPrivateField(institutionalCatalogGovernanceStateRepository, "stateStore"),
                "InstitutionalCatalogGovernanceStateRepository.stateStore deveria vir do construtor real (Spring), nao do degradado");
    }

    /**
     * Mesma classe de bug (0c7e926a) nestes 4 servicos: @Inject no construtor sem args, que criava
     * um ObjectStorageProperties() em branco em vez do bean real @ConfigurationProperties(prefix =
     * "pjb.storage"). Em producao os valores coincidiam com os defaults do Java, o que escondeu o
     * bug — qualquer operador que tentasse configurar um limite via env var (ex.:
     * PJB_STORAGE_UPLOAD_MAX_DOCUMENT_BYTES) teria a config silenciosamente ignorada. Este teste
     * sobrescreve pjb.storage.upload.max-document-bytes para um valor que NAO existe como default
     * em nenhuma classe (12345678, default real e 26214400) e prova, via reflection no campo
     * uploadProperties, que os 4 servicos leem o bean real configurado pelo Spring.
     */
    @Test
    void servicosDeObjectStorageUsamPropertiesRealDoSpringNaoDefaultEmBranco() throws Exception {
        assertEquals(12345678L, uploadMaxDocumentBytes(uploadContentPolicyService));
        assertEquals(12345678L, uploadMaxDocumentBytes(peticionamentoPericiaEvidenceIntelligenceService));
        assertEquals(12345678L, uploadMaxDocumentBytes(peticionamentoMediaPublicationGateService));
        assertEquals(12345678L, uploadMaxDocumentBytes(peticionamentoMediaSecurityPipelineService));
        assertEquals(12345678L, uploadMaxDocumentBytes(peticionamentoMediaStorageShieldService));
    }

    private long uploadMaxDocumentBytes(Object service) throws Exception {
        ObjectStorageProperties.Upload upload = (ObjectStorageProperties.Upload) readPrivateField(service, "uploadProperties");
        return upload.getMaxDocumentBytes();
    }

    private Object readPrivateField(Object target, String fieldName) throws Exception {
        Object real = AopTestUtils.getUltimateTargetObject(target);
        Field field = real.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(real);
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
