package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class DjePublicacaoRepositoryIT extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationDeveCriarTabelaDjePublicacao() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'pjb_dje_publicacao'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
