package com.tcc.pjb.backend.service.recursal.attachments;

import java.time.Instant;

public record RecursalStoredFileRef(
        Long processoId,
        String storageKey,
        String sha256,
        long sizeBytes,
        String contentType,
        String originalFilename,
        Instant uploadedAt
) {
}
