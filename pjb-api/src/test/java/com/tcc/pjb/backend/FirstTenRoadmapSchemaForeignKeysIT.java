package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = BackendApplication.class)
@ActiveProfiles("integration-test")
class FirstTenRoadmapSchemaForeignKeysIT extends PjbIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void shouldContainExpectedForeignKeysForFirstTenRoadmapTables() {
        assertThat(countFk("pjb_mni_remessa", "processo_id")).isGreaterThan(0);
        assertThat(countFk("pjb_mni_recepcao", "processo_id_local")).isGreaterThan(0);
        assertThat(countFk("pjb_audiencia_custodia", "processo_id")).isGreaterThan(0);
        assertThat(countFk("pjb_medida_cautelar", "processo_id")).isGreaterThan(0);
        assertThat(countFk("pjb_sisbajud_operacao", "processo_id")).isGreaterThan(0);
        assertThat(countFk("pjb_custa_judicial", "processo_id")).isGreaterThan(0);
    }

    private int countFk(String tableName, String columnName) {
        Integer value = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.key_column_usage kcu
                join information_schema.table_constraints tc
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_name = kcu.table_name
                 and tc.constraint_schema = kcu.constraint_schema
                where tc.constraint_type = 'FOREIGN KEY'
                  and kcu.table_name = ?
                  and kcu.column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return value == null ? 0 : value;
    }
}
