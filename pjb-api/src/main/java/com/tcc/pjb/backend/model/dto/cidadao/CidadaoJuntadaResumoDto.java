package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.Instant;
import java.util.UUID;

public record CidadaoJuntadaResumoDto(
    long seq,
    Instant createdAt,
    String eventType,
    String label,
    int docCountTotal,
    int docCountVisible,
    UUID eventoId
) {
}
