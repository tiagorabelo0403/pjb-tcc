package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;

public record DiligenceOperationalTimelineEntryResponse(
        String sourceType,
        Long sourceId,
        Instant occurredAt,
        String canal,
        String diligenciaReferencia,
        Long processoId,
        String processoNumero,
        Long workItemId,
        Long processEventSeq,
        String title,
        String status,
        String summary,
        String digestSha256,
        String documentId,
        String bundleReference
) {
}
