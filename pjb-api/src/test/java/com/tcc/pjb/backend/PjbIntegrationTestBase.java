package com.tcc.pjb.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sem configuracao de forkCount/reuseForks no pom, o Failsafe roda todas as subclasses
 * desta base na mesma JVM forked, reaproveitando os mesmos containers POSTGRES/KAFKA estaticos
 * e o mesmo banco entre classes. Nenhum rollback transacional é aplicado por teste (deliberado:
 * cenarios com Kafka listener/Virtual Thread assincrono nao veem dados de uma transacao ainda aberta).
 * Qualquer fixture que grave em tabela de catalogo compartilhado (ex.: tb_jurisdicao_territorial)
 * deve usar um discriminador exclusivo (tribunal_codigo ou municipio_ibge sintetico, nunca um
 * codigo real) para nao vazar para asserts de contagem exata de outras classes do mesmo lote.
 */
@SpringBootTest(classes = BackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public abstract class PjbIntegrationTestBase {

    // pgvector/pgvector:pg17 e postgres:17 + extensao vector precompilada. Necessario porque a
    // migration V307__ai_vector_store_pgvector.sql chama CREATE EXTENSION vector — sem a imagem
    // certa, todo IT falha no Flyway antes de qualquer teste rodar. asCompatibleSubstituteFor
    // ensina o Testcontainers a nao rejeitar a imagem por nao ser "postgres" puro.
    public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("pgvector/pgvector:pg17")
            .asCompatibleSubstituteFor("postgres");

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("pjb_it")
            .withUsername("pjb")
            .withPassword("pjb_test");

    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    // Efêmero por JVM forked: Ryuk (sidecar do Testcontainers) derruba o container assim que o
    // processo Failsafe termina, sem passo manual e sem lixo acumulado entre execuções.
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
            .withExposedPorts(6379);

    static {
        KAFKA.start();
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.datasource.hikari.register-mbeans", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("pjb.kafka.enabled", () -> "true");
        registry.add("pjb.jobs.pg-listen.enabled", () -> "false");
        registry.add("pjb.jobs.dispatcher.enabled", () -> "false");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
