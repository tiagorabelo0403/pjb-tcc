package com.tcc.pjb.backend.it;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Testcontainers
class FlywayPostgresMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pjb")
            .withUsername("pjb")
            .withPassword("pjb");

    @Test
    void migrations_run_cleanly_on_postgres() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            assertTrue(tableExists(c, "pjb_outbox_event"), "Tabela pjb_outbox_event deve existir");
            assertTrue(tableExists(c, "tb_usuario"), "Tabela tb_usuario deve existir");
            assertTrue(tableExists(c, "tb_processo"), "Tabela tb_processo deve existir");
            assertTrue(tableExists(c, "flyway_schema_history"), "Flyway history deve existir");
        }
    }

    private static boolean tableExists(Connection c, String table) throws Exception {
        try (ResultSet rs = c.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        
        try (ResultSet rs = c.getMetaData().getTables(null, null, table.toLowerCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
