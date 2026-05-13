package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FirstTenRoadmapSchemaCoverageIT extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void roadmap_migrations_v178_a_v184_devem_ter_colunas_de_retry_status_e_auditoria() {
        assertColumns("pjb_mni_remessa", "mni_payload_hash", "tentativas", "max_tentativas", "failure_reason");
        assertColumns("pjb_datajud_feed_checkpoint", "last_error", "last_sent_at", "updated_at");
        assertColumns("pjb_sisbajud_operacao", "retorno_bacen", "protocolo_bacen", "tentativas", "proximo_retry_em");
        assertColumns("pjb_custa_judicial", "linha_digitavel", "pix_txid", "vencimento", "pago_em");
        assertColumns("pjb_audiencia_custodia", "preso_nome", "preso_cpf", "medidas_cautelares", "status");
    }

    private void assertColumns(String tableName, String... columns) {
        List<String> actual = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns where table_schema = current_schema() and table_name = ?",
                String.class,
                tableName);
        assertThat(actual).contains(columns);
    }
}
