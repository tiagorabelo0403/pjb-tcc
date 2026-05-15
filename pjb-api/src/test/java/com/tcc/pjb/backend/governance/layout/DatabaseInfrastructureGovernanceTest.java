package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;
import org.junit.jupiter.api.Test;

class DatabaseInfrastructureGovernanceTest {

    private static final Path ROOT = PjbTestPaths.projectRoot();
    private static final Path DOCKER_COMPOSE = ROOT.resolve("docker-compose.yml");
    private static final Path DOCKER_COMPOSE_READ_REPLICA = ROOT.resolve("docker-compose.read-replica.yml");
    private static final Path DOCKER_COMPOSE_HA = ROOT.resolve("docker-compose.ha.yml");
    private static final Path POSTGRES_CONF = ROOT.resolve("infra/docker/postgres/postgresql-pjb.conf");
    private static final Path POSTGRES_INIT = ROOT.resolve("infra/docker/postgres/init/00-extensions-and-hardening.sql");
    private static final Path DB_EDGE = ROOT.resolve("infra/docker/haproxy/db-edge.cfg");
    private static final Path DB_MIGRATION = PjbTestPaths.pjbApiMainResourcesRoot().resolve("db/migration/V172__database_posture_professionalization.sql");

    @Test
    void composeDoBancoDeveUsarPostgres17EConfigProfissional() throws IOException {
        String compose = Files.readString(DOCKER_COMPOSE);
        assertTrue(compose.contains("postgres:17"));
        assertTrue(compose.contains("./infra/docker/postgres/postgresql-pjb.conf:/etc/postgresql/postgresql-pjb.conf:ro"));
        assertTrue(compose.contains("./infra/docker/postgres/init:/docker-entrypoint-initdb.d:ro"));
        assertTrue(compose.contains("config_file=/etc/postgresql/postgresql-pjb.conf"));
    }

    @Test
    void malhaReplicaEHaDevemExporEdgeTcpPgBouncerEEntradaRwRo() throws IOException {
        String readReplica = Files.readString(DOCKER_COMPOSE_READ_REPLICA);
        assertTrue(readReplica.contains("postgres:17"));
        assertTrue(readReplica.contains("/etc/postgresql/postgresql-pjb.conf:ro"));
        assertTrue(readReplica.contains("PJB_DB_READ_ROUTING_ENABLED: \"true\""));

        String ha = Files.readString(DOCKER_COMPOSE_HA);
        assertTrue(ha.contains("db-edge:"));
        assertTrue(ha.contains("pgbouncer-rw:"));
        assertTrue(ha.contains("pgbouncer-ro:"));
        assertTrue(ha.contains("jdbc:postgresql://db-edge:6432/"));
        assertTrue(ha.contains("jdbc:postgresql://db-edge:6433/"));
        assertTrue(ha.contains("PJB_DB_LOAD_BALANCER_MODE: DB_EDGE_TCP"));
    }

    @Test
    void configuracaoPostgresDeveAtivarObservabilidadeESegurancaBasicas() throws IOException {
        String conf = Files.readString(POSTGRES_CONF);
        assertTrue(conf.contains("shared_preload_libraries = 'pg_stat_statements'"));
        assertTrue(conf.contains("compute_query_id = auto"));
        assertTrue(conf.contains("track_io_timing = on"));
        assertTrue(conf.contains("password_encryption = scram-sha-256"));

        String init = Files.readString(POSTGRES_INIT);
        assertTrue(init.contains("CREATE EXTENSION IF NOT EXISTS pg_stat_statements"));
        assertTrue(init.contains("CREATE EXTENSION IF NOT EXISTS pgcrypto"));

        String edge = Files.readString(DB_EDGE);
        assertTrue(edge.contains("frontend pjb_db_rw"));
        assertTrue(edge.contains("frontend pjb_db_ro"));
        assertTrue(edge.contains("backend pjb_db_rw_backend"));
        assertTrue(edge.contains("backend pjb_db_ro_backend"));
    }

    @Test
    void migracaoDePosturaDoBancoDeveSemearParticoesERetencao() throws IOException {
        String migration = Files.readString(DB_MIGRATION);
        assertTrue(migration.contains("tb_database_retention_policy"));
        assertTrue(migration.contains("tb_partition_plan"));
        assertTrue(migration.contains("tb_work_item"));
        assertTrue(migration.contains("tb_outbox_event"));
        assertTrue(migration.contains("tb_processo_event"));
        assertTrue(migration.contains("notification_history"));
    }
}
