package com.tcc.pjb.backend.model.dto.intelligence;

public record RecursalAttachmentUploadResponse(
        Long processoId,
        String storageKey,
        String sha256,
        long sizeBytes,
        String contentType,
        String originalFilename,
        String downloadUrl
) {
}
