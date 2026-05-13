package com.tcc.pjb.backend.service.semantic;

public interface EmbeddingService {
    EmbeddingVector embed(String text);
}
