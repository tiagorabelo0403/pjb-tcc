package com.tcc.pjb.backend;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base para flow ITs (RANDOM_PORT / requisição HTTP real).
 * Não usa @Transactional — a requisição HTTP roda em thread/conexão separada,
 * fora de qualquer TX do teste. Rollback transacional não isola o que a app comita.
 *
 * <p>Isolamento garantido por TRUNCATE CASCADE autodescoberto no @BeforeEach:
 * cada teste entra com banco limpo. A descoberta usa pg_tables filtrando filhas
 * de partição via pg_inherits — zero lista manual de tabelas, zero divergência
 * com futuras migrations.
 *
 * <p>Premissa de segurança do TRUNCATE (ACCESS EXCLUSIVE):
 * O perfil integration-test desabilita schedulers ({@code spring.task.scheduling.enabled=false}),
 * pg-listen ({@code pjb.jobs.pg-listen.enabled=false}) e dispatcher
 * ({@code pjb.jobs.dispatcher.enabled=false}). Entre testes, nenhum TX de background
 * fica aberto — o TRUNCATE não encontra bloqueio. Se algum IT habilitar job de background
 * no perfil integration-test, o TRUNCATE pode travar aguardando o lock ACCESS EXCLUSIVE.
 * Manter os jobs desabilitados é pré-requisito para herdar esta base.
 */
public abstract class PjbFlowItBase extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void truncateDatabaseBeforeEach() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT t.tablename
                FROM pg_tables t
                WHERE t.schemaname = 'public'
                  AND t.tablename NOT IN ('flyway_schema_history')
                  AND t.tablename NOT IN (
                      SELECT c.relname FROM pg_inherits i
                      JOIN pg_class c ON i.inhrelid = c.oid
                      JOIN pg_namespace n ON c.relnamespace = n.oid
                      WHERE n.nspname = 'public'
                  )
                """,
                String.class);
        if (!tables.isEmpty()) {
            String tableList = String.join(", ", tables.stream().map(t -> "\"" + t + "\"").toList());
            jdbcTemplate.execute("TRUNCATE " + tableList + " RESTART IDENTITY CASCADE");
        }
    }
}
