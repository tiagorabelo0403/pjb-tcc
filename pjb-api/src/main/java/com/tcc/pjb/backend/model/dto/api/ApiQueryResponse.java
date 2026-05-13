package com.tcc.pjb.backend.model.dto.api;

import java.time.Instant;
import java.util.List;

public record ApiQueryResponse<T>(
        String status,
        String correlationId,
        T data,
        List<String> warnings,
        Instant generatedAt
) {
    public ApiQueryResponse {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
