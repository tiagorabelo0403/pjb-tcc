package com.tcc.pjb.backend.core.financeiro.custas;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class CustaJudicialRepositoryIT extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationDeveCriarTabelaCustaJudicial() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'pjb_custa_judicial'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
