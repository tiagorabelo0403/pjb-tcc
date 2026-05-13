package com.tcc.pjb.backend.model.dto.processo;

import java.time.Instant;
import java.util.List;

public record ProcessoNoteDto(
    Long id,
    Long processoId,
    Long authorUsuarioId,
    String authorTipo,
    String body,
    List<String> tags,
    Instant createdAt,
    Instant updatedAt
) {
}
