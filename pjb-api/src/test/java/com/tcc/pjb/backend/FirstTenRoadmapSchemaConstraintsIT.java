package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FirstTenRoadmapSchemaConstraintsIT extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void roadmap_migrations_v178_a_v184_devem_ter_constraints_unicas_e_indices_criticos_adicionais() {
        assertConstraint("pjb_icp_certificate_cache", "uk_icp_cert_serial");
        assertConstraint("pjb_mni_remessa", "uk_mni_remessa_processo_destino");
        assertConstraint("pjb_mni_recepcao", "uk_mni_recepcao_hash");
        assertIndex("pjb_medida_cautelar", "idx_medida_ativa");
        assertIndex("pjb_sisbajud_operacao", "idx_sisbajud_status");
        assertIndex("pjb_renajud_restricao", "idx_renajud_processo");
        assertIndex("pjb_gru_judicial_trabalhista", "idx_gru_trab_processo");
    }

    private void assertConstraint(String tableName, String constraintName) {
        List<String> actual = jdbcTemplate.queryForList(
                "select constraint_name from information_schema.table_constraints where table_schema = current_schema() and table_name = ?",
                String.class,
                tableName);
        assertThat(actual).contains(constraintName);
    }

    private void assertIndex(String tableName, String indexName) {
        List<String> actual = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = current_schema() and tablename = ?",
                String.class,
                tableName);
        assertThat(actual).contains(indexName);
    }
}
