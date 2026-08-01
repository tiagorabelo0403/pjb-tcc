package com.tcc.pjb.backend.service.semantic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacao persistente do {@link VectorIndex} sobre a extensao pgvector (schema criado pela
 * migration V307). Ativada por {@code pjb.ai.vector.mode=pgvector}; sem essa flag, o Spring
 * mantem o {@link InMemoryCosineVectorIndex} como default.
 *
 * <p>Sob o mesmo store {@code pjb_ai_vector_document} usado pelo {@code VectorSearchServicePgVector},
 * de modo que ingest via {@code SemanticPrecedentSearchService} e busca via {@code VectorSearchService}
 * compartilham dados naturalmente — o precedente indexado por um caminho fica disponivel para o outro.
 *
 * <p>Dimensao ajustada por truncamento/padding + renormalizacao (mesma logica do adapter);
 * metadata {@code Map<String,String>} vira {@code jsonb} preservando o contrato case-insensitive do
 * matcher da interface (filtros do search sao normalizados para lowercase antes de irem para o
 * WHERE {@code metadata @> ?::jsonb}).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "pjb.ai.vector", name = "mode", havingValue = "pgvector")
public class PgVectorPersistentIndex implements VectorIndex {

    private static final int PGVECTOR_TARGET_DIMENSION = 1536;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int targetDimension;

    public PgVectorPersistentIndex(JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${pjb.ai.vector.pgvector.target-dimension:" + PGVECTOR_TARGET_DIMENSION + "}") int targetDimension) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.targetDimension = targetDimension > 0 ? targetDimension : PGVECTOR_TARGET_DIMENSION;
    }

    @Override
    public void upsert(String id, EmbeddingVector vector, Map<String, String> metadata) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(vector, "vector");
        Map<String, String> lowerMeta = normalizeMetadataForCaseInsensitiveMatch(metadata);
        String metaJson = toJson(lowerMeta);
        float[] adjusted = adjustDimension(vector.normalized().values());
        String pgLiteral = toPgVectorLiteral(adjusted);
        String titulo = lowerMeta.getOrDefault("titulo", id);
        String ramo = lowerMeta.get("ramo");
        String conteudo = lowerMeta.getOrDefault("conteudo", id);
        jdbcTemplate.update(
                "INSERT INTO pjb_ai_vector_document (doc_id, titulo, ramo, conteudo, embedding, metadata) "
                        + "VALUES (?, ?, ?, ?, ?::vector, ?::jsonb) "
                        + "ON CONFLICT (doc_id) DO UPDATE SET "
                        + "  titulo = EXCLUDED.titulo, ramo = EXCLUDED.ramo, conteudo = EXCLUDED.conteudo, "
                        + "  embedding = EXCLUDED.embedding, metadata = EXCLUDED.metadata",
                id, titulo, ramo, conteudo, pgLiteral, metaJson);
    }

    @Override
    public List<VectorSearchHit> search(EmbeddingVector query, int topK, Map<String, String> filter) {
        Objects.requireNonNull(query, "query");
        int k = Math.max(1, topK);
        float[] adjusted = adjustDimension(query.normalized().values());
        String pgLiteral = toPgVectorLiteral(adjusted);
        String filterJson = filter == null || filter.isEmpty()
                ? null
                : toJson(normalizeMetadataForCaseInsensitiveMatch(filter));

        StringBuilder sql = new StringBuilder(256);
        sql.append("SELECT doc_id, metadata, (embedding <=> ?::vector) AS distance ")
           .append("FROM pjb_ai_vector_document");
        List<Object> params = new ArrayList<>(3);
        params.add(pgLiteral);
        if (filterJson != null) {
            sql.append(" WHERE metadata @> ?::jsonb");
            params.add(filterJson);
        }
        sql.append(" ORDER BY embedding <=> ?::vector ASC LIMIT ?");
        params.add(pgLiteral);
        params.add(k);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            String docId = rs.getString("doc_id");
            String metaJson = rs.getString("metadata");
            double distance = rs.getDouble("distance");
            float score = (float) Math.max(0.0, 1.0 - distance);
            return new VectorSearchHit(docId, score, parseMetadata(metaJson));
        });
    }

    @Override
    public int size() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pjb_ai_vector_document", Integer.class);
        return count == null ? 0 : count;
    }

    private float[] adjustDimension(float[] source) {
        if (source.length == targetDimension) {
            return source;
        }
        float[] adjusted = new float[targetDimension];
        int copyLen = Math.min(source.length, targetDimension);
        System.arraycopy(source, 0, adjusted, 0, copyLen);
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

    private static Map<String, String> normalizeMetadataForCaseInsensitiveMatch(Map<String, String> in) {
        if (in == null || in.isEmpty()) return Map.of();
        LinkedHashMap<String, String> out = new LinkedHashMap<>(in.size());
        for (Map.Entry<String, String> entry : in.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            out.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar metadata para JSONB", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(metaJson, Map.class);
        } catch (Exception ex) {
            log.warn("metadata jsonb malformado ao ler pjb_ai_vector_document: {}", ex.getMessage());
            return Map.of();
        }
    }
}
