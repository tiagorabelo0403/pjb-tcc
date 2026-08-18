package com.tcc.pjb.backend.db.migration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class OrganizacaoJudiciariaBackfillGuardIT {

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(PjbIntegrationTestBase.POSTGRES_IMAGE)
                    .withDatabaseName("pjb_guard_teste")
                    .withUsername("pjb")
                    .withPassword("pjb_guard_teste");

    @Test
    void bloqueiaV320QuandoComarcaTextualNaoResolveContraOCatalogo() throws Exception {
        Flyway ateTask1 = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("319")
                .load();
        ateTask1.migrate();

        try (Connection conexao = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                Statement statement = conexao.createStatement()) {
            statement.execute(
                    """
                    INSERT INTO tb_unidade_judiciaria_competencia
                        (codigo, tribunal_codigo, comarca, uf, tipo_vara)
                    VALUES
                        ('UNIDADE-GUARD-TESTE-XYZ123', 'TRT7', 'Cidade Inexistente XYZ123', 'CE', 'VARA_UNICA')
                    """);
        }

        Flyway ateTask2 = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("320")
                .load();

        assertThatThrownBy(ateTask2::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("Backfill de comarca em tb_unidade_judiciaria_competencia incompleto");
    }

    @Test
    void migraTodasAsVersoesSemPendenciaDeBackfillSemLancarExcecao() {
        Flyway flywayCompleto = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThatCode(flywayCompleto::migrate).doesNotThrowAnyException();
    }
}
