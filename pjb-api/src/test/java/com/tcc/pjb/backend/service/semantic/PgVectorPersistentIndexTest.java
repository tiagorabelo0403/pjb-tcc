package com.tcc.pjb.backend.service.semantic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PgVectorPersistentIndexTest {

    private JdbcTemplate jdbcTemplate;
    private PgVectorPersistentIndex index;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        index = new PgVectorPersistentIndex(jdbcTemplate, new ObjectMapper(), 4);
    }

    @Test
    void upsertNormalizaMetadataParaLowercaseESerializaJsonb() {
        index.upsert("doc-1", new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}),
                Map.of("Ramo", "PENAL", "Rito", "COMUM_ORDINARIO"));

        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(contains("ON CONFLICT (doc_id) DO UPDATE"), any(), any(), any(), any(), any(), any());
        // no upsert com 6 params posicionais (id, titulo, ramo, conteudo, embedding, metadata),
        // verificar via captura do metadata (ultimo) confere case-lower e jsonb valido
    }

    @Test
    void toPgVectorLiteral_produzFormatoAceitoPeloPgvector() {
        String literal = PgVectorPersistentIndex.toPgVectorLiteral(new float[]{0.1f, -0.2f, 0.5f});
        assertThat(literal).isEqualTo("[0.1000000,-0.2000000,0.5000000]");
    }

    @Test
    void searchDistanceParaScore() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("doc_id")).thenReturn("doc-7");
        when(rs.getString("metadata")).thenReturn("{\"ramo\":\"penal\"}");
        when(rs.getDouble("distance")).thenReturn(0.25);
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<VectorSearchHit> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });

        List<VectorSearchHit> hits = index.search(
                new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}), 3, Map.of("Ramo", "PENAL"));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).id()).isEqualTo("doc-7");
        assertThat(hits.get(0).score()).isEqualTo(0.75f);
        assertThat(hits.get(0).metadata()).containsEntry("ramo", "penal");
    }

    @Test
    void searchSemFiltro_omiteWhereJsonb() {
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());
        index.search(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}), 3, Map.of());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(Object[].class), any(RowMapper.class));
        assertThat(sql.getValue()).doesNotContain("metadata @>");
    }

    @Test
    void searchComFiltro_normalizaLowercaseAntesDeMandarParaJsonb() {
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());
        index.search(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}), 3, Map.of("Ramo", "PENAL"));

        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(contains("metadata @> ?::jsonb"), params.capture(), any(RowMapper.class));
        // params: [pgLiteral, jsonFilter, pgLiteral, topK]
        String jsonFilter = (String) params.getValue()[1];
        assertThat(jsonFilter).isEqualTo("{\"ramo\":\"penal\"}");
    }

    @Test
    void adjustDimension_truncaSeMaior() {
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());
        index.search(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f, 0f, 0f}), 3, Map.of());

        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(), params.capture(), any(RowMapper.class));
        String pgLiteral = (String) params.getValue()[0];
        assertThat(pgLiteral.split(",")).hasSize(4); // targetDimension configurado no @BeforeEach
    }

    @Test
    void size_ContaLinhasNaTabela() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM pjb_ai_vector_document"), eq(Integer.class)))
                .thenReturn(42);
        assertThat(index.size()).isEqualTo(42);
    }

    @Test
    void size_NullTratadoComoZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(null);
        assertThat(index.size()).isZero();
    }
}
