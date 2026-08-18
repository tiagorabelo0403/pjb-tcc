package com.tcc.pjb.backend.ai.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.service.semantic.EmbeddingService;
import com.tcc.pjb.backend.service.semantic.EmbeddingVector;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class VectorSearchServicePgVectorTest {

    private JdbcTemplate jdbcTemplate;
    private EmbeddingService embeddingService;
    private VectorSearchServicePgVector service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        embeddingService = mock(EmbeddingService.class);
        service = new VectorSearchServicePgVector(jdbcTemplate, embeddingService, new ObjectMapper(), 4, 5);
    }

    @Test
    void toPgVectorLiteral_produzFormatoAceitoPeloPgvector() {
        String literal = VectorSearchServicePgVector.toPgVectorLiteral(new float[]{0.1f, -0.2f, 0.5f});
        assertThat(literal).isEqualTo("[0.1000000,-0.2000000,0.5000000]");
    }

    @Test
    void ajustaDimensaoTruncandoQuandoOEmbeddingEMaior() throws Exception {
        when(embeddingService.embed(anyString())).thenReturn(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f}));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());

        service.searchSimilarResult("q", Map.of(), 3);

        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));
        String pgLiteral = (String) paramsCaptor.getValue()[0];
        // literal deve ter exatamente 4 componentes (targetDimension configurado no @BeforeEach)
        assertThat(pgLiteral.split(",")).hasSize(4);
    }

    @Test
    void searchSimilarResult_mapeiaDistanciaParaScore() throws Exception {
        when(embeddingService.embed(anyString())).thenReturn(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("doc_id")).thenReturn("doc-1");
        when(rs.getString("titulo")).thenReturn("Titulo teste");
        when(rs.getString("ramo")).thenReturn("PENAL");
        when(rs.getDouble("distance")).thenReturn(0.25);
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<VectorSearchService.ResultItem> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });

        VectorSearchService.VectorSearchResult result = service.searchSimilarResult("q", Map.of(), 3);

        assertThat(result.resultados()).hasSize(1);
        VectorSearchService.ResultItem item = result.resultados().get(0);
        assertThat(item.docId()).isEqualTo("doc-1");
        assertThat(item.titulo()).isEqualTo("Titulo teste");
        assertThat(item.ramo()).isEqualTo("PENAL");
        assertThat(item.score()).isEqualTo(0.75); // 1 - 0.25
        assertThat(item.cosine()).isEqualTo(0.75);
        assertThat(result.iaVersion()).isEqualTo("v-current");
    }

    @Test
    void filtroMetadataViraJsonbNoWhere() {
        when(embeddingService.embed(anyString())).thenReturn(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());

        service.searchSimilarResult("q", Map.of("ramo", "PENAL"), 3);

        verify(jdbcTemplate).query(contains("metadata @> ?::jsonb"), any(Object[].class), any(RowMapper.class));
    }

    @Test
    void semFiltro_naoAdicionaWhereJsonb() {
        when(embeddingService.embed(anyString())).thenReturn(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());

        service.searchSimilarResult("q", Map.of(), 3);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).doesNotContain("metadata @>");
    }

    @Test
    void erroDoJdbcRetornaResultadoDegradadoSemLancar() {
        when(embeddingService.embed(anyString())).thenReturn(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("pgvector down"));

        VectorSearchService.VectorSearchResult result = service.searchSimilarResult("q", Map.of(), 3);

        assertThat(result.resultados()).isEmpty();
        assertThat(result.iaVersion()).isEqualTo("pgvector-error");
        assertThat(result.explicabilidade()).containsEntry("degraded", true);
    }

    @Test
    void topKZero_usaDefaultConfigurado() {
        when(embeddingService.embed(anyString())).thenReturn(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());

        service.searchSimilarResult("q", Map.of(), 0);

        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));
        Object[] params = paramsCaptor.getValue();
        // ultimo parametro e o LIMIT (topK)
        assertThat(params[params.length - 1]).isEqualTo(5);
    }

    @Test
    void versoesV1V2V3_marcamIaVersionCorreta() {
        when(embeddingService.embed(anyString())).thenReturn(new EmbeddingVector(new float[]{1f, 0f, 0f, 0f}));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(service.searchSimilarV1("q", Map.of(), 3).iaVersion()).isEqualTo("v1");
        assertThat(service.searchSimilarV2("q", Map.of(), 3).iaVersion()).isEqualTo("v2");
        assertThat(service.searchSimilarV3("q", Map.of(), 3).iaVersion()).isEqualTo("v3");
    }
}
