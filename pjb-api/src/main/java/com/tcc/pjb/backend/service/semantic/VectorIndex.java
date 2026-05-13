package com.tcc.pjb.backend.service.semantic;

import java.util.List;
import java.util.Map;

public interface VectorIndex {

    void upsert(String id, EmbeddingVector vector, Map<String, String> metadata);

    List<VectorSearchHit> search(EmbeddingVector query, int topK, Map<String, String> filter);

    int size();
}
