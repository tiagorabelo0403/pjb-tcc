package com.tcc.pjb.backend.model.dto.processual.validation.material;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MaterialLegalValidationResponse(
        Long processoId,
        String numeroProcesso,
        String action,
        boolean permitido,
        String competenciaStatus,
        String faseAtual,
        String statusAtual,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata,
        Instant validatedAt
) {
    public MaterialLegalValidationResponse {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        validatedAt = validatedAt == null ? Instant.now() : validatedAt;
    }
}
