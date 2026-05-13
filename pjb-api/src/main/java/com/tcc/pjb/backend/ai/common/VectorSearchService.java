package com.tcc.pjb.backend.ai.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface VectorSearchService {

    
    VectorSearchResult searchSimilarResult(String query, Map<String, Object> filtros, int topK);

    
    VectorSearchResult searchSimilarV1(String query, Map<String, Object> filtros, int topK); 
    VectorSearchResult searchSimilarV2(String query, Map<String, Object> filtros, int topK); 
    VectorSearchResult searchSimilarV3(String query, Map<String, Object> filtros, int topK); 

    
    default String searchSimilar(String query, Map<String, Object> filtros, int topK) {
        return VectorSearchSerializer.toJson(searchSimilarResult(query, filtros, topK));
    }

    
    record VectorSearchResult(
            String query,
            Instant timestamp,
            List<ResultItem> resultados,
            Map<String, Object> explicabilidade,
            Map<String, Object> auditoria,
            String iaVersion
    ) { }

    
    record ResultItem(
            String docId,
            String titulo,
            String ramo,
            double score,
            double cosine,
            double boost
    ) { }
}
