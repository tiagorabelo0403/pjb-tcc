package com.tcc.pjb.backend.model.dto.api;

import java.time.Instant;
import java.util.List;

public record ApiCommandResponse<T>(
        String status,
        String correlationId,
        String message,
        T data,
        List<String> warnings,
        Instant generatedAt
) {
    public ApiCommandResponse {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
