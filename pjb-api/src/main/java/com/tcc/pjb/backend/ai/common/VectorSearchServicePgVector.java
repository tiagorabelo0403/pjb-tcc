package com.tcc.pjb.backend.ai.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.service.semantic.EmbeddingService;
import com.tcc.pjb.backend.service.semantic.EmbeddingVector;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Adapter de producao do {@link VectorSearchService} sobre pgvector (extensao Postgres,
 * habilitada pela migration V307). Ativado por {@code pjb.ai.vector.mode=pgvector}; sem
 * essa flag, o modo continua {@code disabled} (default) ou {@code mock} (dev/test).
 *
 * <p>A dimensao do vetor no banco (coluna {@code embedding vector(1536)}) e fixada em
 * {@link #targetDimension}; se o {@link EmbeddingService} configurado devolver um vetor
 * de dimensao diferente, ele e adaptado por truncamento/padding e renormalizado.
 * Simples, evita quebrar quando o operador troca de modelo sem migrar o schema.
 *
 * <p>O calculo de score usa a distancia de cosseno do pgvector (operador {@code <=>}):
 * {@code score = 1 - distancia}, com filtragem por {@code metadata jsonb} quando o mapa
 * de filtros for nao vazio (indice GIN sobre metadata cobre isso sem full scan).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "pjb.ai.vector", name = "mode", havingValue = "pgvector")
public class VectorSearchServicePgVector implements VectorSearchService {

    private static final int PGVECTOR_TARGET_DIMENSION = 1536;
    private static final double MIN_SCORE = 0.0;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final int targetDimension;
    private final int defaultTopK;

    public VectorSearchServicePgVector(JdbcTemplate jdbcTemplate,
                                       EmbeddingService embeddingService,
                                       ObjectMapper objectMapper,
                                       @Value("${pjb.ai.vector.pgvector.target-dimension:" + PGVECTOR_TARGET_DIMENSION + "}") int targetDimension,
                                       @Value("${pjb.ai.vector.pgvector.default-top-k:5}") int defaultTopK) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.embeddingService = Objects.requireNonNull(embeddingService, "embeddingService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.targetDimension = targetDimension > 0 ? targetDimension : PGVECTOR_TARGET_DIMENSION;
        this.defaultTopK = defaultTopK > 0 ? defaultTopK : 5;
    }

    @Override
    public VectorSearchResult searchSimilarResult(String query, Map<String, Object> filtros, int topK) {
        return doSearch(query, filtros, topK, "v-current");
    }

    @Override
    public VectorSearchResult searchSimilarV1(String query, Map<String, Object> filtros, int topK) {
        return doSearch(query, filtros, topK, "v1");
    }

    @Override
    public VectorSearchResult searchSimilarV2(String query, Map<String, Object> filtros, int topK) {
        return doSearch(query, filtros, topK, "v2");
    }

    @Override
    public VectorSearchResult searchSimilarV3(String query, Map<String, Object> filtros, int topK) {
        return doSearch(query, filtros, topK, "v3");
    }

    private VectorSearchResult doSearch(String query, Map<String, Object> filtros, int topK, String iaVersion) {
        String safeQuery = query == null ? "" : query;
        int effectiveTopK = topK > 0 ? topK : defaultTopK;
        Instant startedAt = Instant.now();
        try {
            float[] adjusted = adjustDimension(embeddingService.embed(safeQuery));
            String pgLiteral = toPgVectorLiteral(adjusted);
            String metadataFilterJson = extractMetadataFilterJson(filtros);
            List<ResultItem> items = executeQuery(pgLiteral, metadataFilterJson, effectiveTopK);
            return new VectorSearchResult(
                    safeQuery,
                    startedAt,
                    items,
                    Map.of(
                            "backend", "pgvector",
                            "targetDimension", targetDimension,
                            "topK", effectiveTopK,
                            "metadataFiltered", metadataFilterJson != null),
                    Map.of(
                            "elapsedMs", Instant.now().toEpochMilli() - startedAt.toEpochMilli(),
                            "iaVersion", iaVersion),
                    iaVersion);
        } catch (RuntimeException ex) {
            log.warn("VectorSearchServicePgVector falha em consulta (topK={}): {}", effectiveTopK, ex.getMessage());
            return new VectorSearchResult(
                    safeQuery,
                    startedAt,
                    List.of(),
                    Map.of("backend", "pgvector", "degraded", true),
                    Map.of("error", ex.getClass().getSimpleName(), "reason", String.valueOf(ex.getMessage())),
                    "pgvector-error");
        }
    }

    private List<ResultItem> executeQuery(String pgVectorLiteral, String metadataFilterJson, int topK) {
        StringBuilder sql = new StringBuilder(256);
        sql.append("SELECT doc_id, titulo, ramo, (embedding <=> ?::vector) AS distance ")
           .append("FROM pjb_ai_vector_document");
        List<Object> params = new ArrayList<>(3);
        params.add(pgVectorLiteral);
        if (metadataFilterJson != null) {
            sql.append(" WHERE metadata @> ?::jsonb");
            params.add(metadataFilterJson);
        }
        sql.append(" ORDER BY embedding <=> ?::vector ASC LIMIT ?");
        params.add(pgVectorLiteral);
        params.add(topK);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            String docId = rs.getString("doc_id");
            String titulo = rs.getString("titulo");
            String ramo = rs.getString("ramo");
            double distance = rs.getDouble("distance");
            double score = Math.max(MIN_SCORE, 1.0 - distance);
            return new ResultItem(docId, titulo, ramo, score, 1.0 - distance, 0.0);
        });
    }

    private float[] adjustDimension(EmbeddingVector source) {
        Objects.requireNonNull(source, "embedding");
        float[] values = source.values();
        if (values.length == targetDimension) {
            return values;
        }
        float[] adjusted = new float[targetDimension];
        int copyLen = Math.min(values.length, targetDimension);
        System.arraycopy(values, 0, adjusted, 0, copyLen);
        double norm = 0.0;
        for (float v : adjusted) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0.0) {
            for (int i = 0; i < adjusted.length; i++) {
                adjusted[i] = (float) (adjusted[i] / norm);
            }
        }
        return adjusted;
    }

    static String toPgVectorLiteral(float[] values) {
        StringBuilder sb = new StringBuilder(values.length * 8);
        sb.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.7f", values[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    private String extractMetadataFilterJson(Map<String, Object> filtros) {
        if (filtros == null || filtros.isEmpty()) {
            return null;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : filtros.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(entry.getKey(), entry.getValue());
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            log.warn("Filtros nao serializaveis para JSONB: {}", ex.getMessage());
            return null;
        }
    }
}
