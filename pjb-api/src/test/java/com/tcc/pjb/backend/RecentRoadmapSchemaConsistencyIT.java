package com.tcc.pjb.backend;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RecentRoadmapSchemaConsistencyIT extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void roadmap_migrations_v189_a_v200_devem_ter_colunas_indices_e_fks_criticos() {
        assertColumns("pjb_dje_publicacao", "processo_id", "status", "data_publicacao", "prazo_comeca_em", "partes_notificadas");
        assertColumns("pjb_sobrestamento_tema", "processo_id", "tema_id", "status_anterior", "retomado_em");
        assertColumns("pjb_calendario_eleitoral", "ano_eleitoral", "tipo_eleicao", "fase", "uf");
        assertColumns("pjb_mni_remessa", "processo_id", "tribunal_destino", "status", "proximo_retry_em", "tentativas");
        assertColumns("pjb_datajud_feed_checkpoint", "tribunal_codigo", "last_processo_id", "total_sent", "updated_at");
        assertColumns("pjb_digitalizacao_job", "processo_id", "status", "operador_id", "revisao_requerida");
        assertColumns("pjb_audiencia_custodia", "processo_id", "magistrado_id", "prazo_limite_24h", "resultado");
        assertColumns("pjb_custa_judicial", "processo_id", "status", "pix_txid", "vencimento");
        assertColumns("pjb_icp_certificate_cache", "issuer_dn", "serial_hex", "valid_until", "revoked");
        assertColumns("pjb_sisbajud_operacao", "processo_id", "operador_id", "authz_trail_id", "status", "cpf_devedor", "proximo_retry_em");
        assertColumns("pjb_gru_judicial_trabalhista", "processo_id", "tipo", "linha_digitavel", "tribunal_trt");
        assertColumns("pjb_renajud_restricao", "tentativas", "proximo_retry_em", "confirmado_em");
        assertColumns("pjb_infojud_consulta", "tentativas", "proximo_retry_em", "confirmado_em");

        assertIndex("pjb_dje_publicacao", "idx_dje_status");
        assertIndex("pjb_mni_remessa", "idx_mni_remessa_status");
        assertIndex("pjb_custa_judicial", "idx_custa_status");
        assertIndex("pjb_sisbajud_operacao", "idx_sisbajud_status");
        assertIndex("pjb_infojud_consulta", "idx_infojud_status");
        assertIndex("pjb_sisbajud_operacao", "idx_sisbajud_retry");
        assertIndex("pjb_renajud_restricao", "idx_renajud_retry");
        assertIndex("pjb_infojud_consulta", "idx_infojud_retry");

        assertForeignKey("pjb_sobrestamento_tema", "operador_id", "tb_usuario");
        assertForeignKey("pjb_digitalizacao_job", "operador_id", "tb_usuario");
        assertForeignKey("pjb_audiencia_custodia", "magistrado_id", "tb_usuario");
        assertForeignKey("pjb_bnmp_consulta_log", "operador_id", "tb_usuario");
        assertForeignKey("pjb_sisbajud_operacao", "operador_id", "tb_usuario");
        assertForeignKey("pjb_renajud_restricao", "operador_id", "tb_usuario");
        assertForeignKey("pjb_infojud_consulta", "operador_id", "tb_usuario");
    }

    private void assertColumns(String tableName, String... columns) {
        List<String> actual = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns where table_schema = current_schema() and table_name = ?",
                String.class,
                tableName);
        assertThat(actual).contains(columns);
    }

    private void assertIndex(String tableName, String indexName) {
        List<String> actual = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = current_schema() and tablename = ?",
                String.class,
                tableName);
        assertThat(actual).contains(indexName);
    }

    private void assertForeignKey(String tableName, String columnName, String targetTable) {
        List<String> actual = jdbcTemplate.queryForList(
                "select ccu.table_name from information_schema.table_constraints tc " +
                        "join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema " +
                        "join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name and ccu.table_schema = tc.table_schema " +
                        "where tc.constraint_type = 'FOREIGN KEY' and tc.table_schema = current_schema() and tc.table_name = ? and kcu.column_name = ?",
                String.class,
                tableName,
                columnName);
        assertThat(actual).contains(targetTable);
    }
}
