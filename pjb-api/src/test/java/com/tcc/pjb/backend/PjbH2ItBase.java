package com.tcc.pjb.backend;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PjbH2ItBase — isolamento de contexto H2 para o módulo advocacia enquanto as
 * classes filhas rodam em H2 sem Flyway (spring.profiles.active=test, ddl-auto=create-drop).
 * PONTE até a D23 (migração H2 → Testcontainers Postgres + PjbFlowItBase). REMOVER quando a
 * D23 migrar essas classes. NÃO estender para novos ITs — novos ITs usam PjbFlowItBase (Postgres).
 */
public abstract class PjbH2ItBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void truncarAntesDoTeste() {
        truncarTabelas();
    }

    @AfterEach
    void truncarDepoisDoTeste() {
        truncarTabelas();
    }

    private void truncarTabelas() {
        List<String> tabelas = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                String.class);
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            tabelas.forEach(tabela -> jdbcTemplate.execute("TRUNCATE TABLE \"" + tabela + "\" RESTART IDENTITY"));
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
