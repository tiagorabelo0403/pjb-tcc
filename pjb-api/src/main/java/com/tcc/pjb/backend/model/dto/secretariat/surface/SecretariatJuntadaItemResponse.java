package com.tcc.pjb.backend.model.dto.secretariat.surface;

import java.time.Instant;
import java.util.UUID;

public record SecretariatJuntadaItemResponse(
        long seq,
        Instant createdAt,
        String eventType,
        String label,
        int docCount,
        UUID eventoId
) {
}
