package com.tcc.pjb.backend.ai.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "pjb.ai.vector", name = "mode", havingValue = "disabled", matchIfMissing = true)
public class VectorSearchServiceDisabled implements VectorSearchService {

    @Override
    public VectorSearchResult searchSimilarResult(String query, Map<String, Object> filtros, int topK) {
        return empty(query);
    }

    @Override
    public VectorSearchResult searchSimilarV1(String query, Map<String, Object> filtros, int topK) {
        return empty(query);
    }

    @Override
    public VectorSearchResult searchSimilarV2(String query, Map<String, Object> filtros, int topK) {
        return empty(query);
    }

    @Override
    public VectorSearchResult searchSimilarV3(String query, Map<String, Object> filtros, int topK) {
        return empty(query);
    }

    private static VectorSearchResult empty(String query) {
        return new VectorSearchResult(
                query,
                Instant.now(),
                List.of(),
                Map.of("disabled", true),
                Map.of("reason", "pjb.ai.vector.mode=disabled"),
                "disabled"
        );
    }
}
