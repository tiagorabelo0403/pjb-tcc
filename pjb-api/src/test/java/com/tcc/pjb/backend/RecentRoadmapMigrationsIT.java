package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RecentRoadmapMigrationsIT extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExposeRecentRoadmapTables() {
        List<String> tables = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'pjb_dje_publicacao',
                    'pjb_sobrestamento_tema',
                    'pjb_calendario_eleitoral',
                    'pjb_feito_eleitoral_especial',
                    'pjb_mni_remessa',
                    'pjb_datajud_feed_checkpoint',
                    'pjb_digitalizacao_job',
                    'pjb_audiencia_custodia',
                    'pjb_custa_judicial',
                    'pjb_icp_certificate_cache',
                    'pjb_sisbajud_operacao',
                    'pjb_gru_judicial_trabalhista'
                  )
                order by table_name
                """, String.class);
        assertThat(tables).contains(
                "pjb_dje_publicacao",
                "pjb_sobrestamento_tema",
                "pjb_calendario_eleitoral",
                "pjb_feito_eleitoral_especial",
                "pjb_mni_remessa",
                "pjb_datajud_feed_checkpoint",
                "pjb_digitalizacao_job",
                "pjb_audiencia_custodia",
                "pjb_custa_judicial",
                "pjb_icp_certificate_cache",
                "pjb_sisbajud_operacao",
                "pjb_gru_judicial_trabalhista");
    }
}
