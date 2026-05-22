package com.tcc.pjb.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ProcessoSigiloSecureReadModelIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PjbIntegrationTestBase.POSTGRES_IMAGE)
            .withDatabaseName("pjb")
            .withUsername("pjb")
            .withPassword("pjb");

    @Test
    void secureReadModelDeveRespeitarScopeTribunalUnidadeEClearance() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            insertProcesso(c, 1L, "0001", "PUBLICO", "TJCE", "UNID-1");
            insertProcesso(c, 2L, "0002", "SIGILO_N2", "TJCE", "UNID-1");
            insertProcesso(c, 3L, "0003", "SEGREDO_ESTADO", "TJCE", "UNID-9");

            assertThat(idsFromSecureView(c)).containsExactly(1L);

            applySessionScope(c, "SIGILO_N2", "TJCE", "UNID-1", "PROC_SIGILO|TJCE|UNID-1|SIGILO_N2");
            assertThat(idsFromSecureView(c)).containsExactly(1L, 2L);

            applySessionScope(c, "SEGREDO_ESTADO", "TJCE", "UNID-9", "PROC_SIGILO|TJCE|UNID-9|SEGREDO_ESTADO");
            assertThat(idsFromSecureView(c)).containsExactly(1L, 3L);
        }
    }

    private void insertProcesso(Connection c,
                                Long id,
                                String numeroUnificado,
                                String nivelSigilo,
                                String tribunal,
                                String unidadeJudiciariaCodigo) throws Exception {
        try (PreparedStatement stmt = c.prepareStatement("""
                INSERT INTO tb_processo (
                    id,
                    numero_unificado,
                    numero_processo,
                    tribunal,
                    unidade_judiciaria_codigo,
                    nivel_sigilo,
                    status_processo,
                    data_criacao,
                    data_atualizacao,
                    data_ultima_movimentacao
                ) VALUES (?, ?, ?, ?, ?, ?, 'EM_ANDAMENTO', now(), now(), now())
                """)) {
            stmt.setLong(1, id);
            stmt.setString(2, numeroUnificado);
            stmt.setString(3, numeroUnificado);
            stmt.setString(4, tribunal);
            stmt.setString(5, unidadeJudiciariaCodigo);
            stmt.setString(6, nivelSigilo);
            stmt.executeUpdate();
        }
    }

    private void applySessionScope(Connection c,
                                   String clearance,
                                   String tribunal,
                                   String unidade,
                                   String scope) throws Exception {
        execute(c, "SET app.pjb_sigilo_clearance = '" + clearance + "'");
        execute(c, "SET app.pjb_tribunal_code = '" + tribunal + "'");
        execute(c, "SET app.pjb_unit_code = '" + unidade + "'");
        execute(c, "SET app.pjb_sigilo_scope = '" + scope + "'");
    }

    private void execute(Connection c, String sql) throws Exception {
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    private List<Long> idsFromSecureView(Connection c) throws Exception {
        ArrayList<Long> out = new ArrayList<>();
        try (PreparedStatement stmt = c.prepareStatement("SELECT id FROM vw_pjb_processo_sigilo_secure ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                out.add(rs.getLong(1));
            }
        }
        return List.copyOf(out);
    }
}
