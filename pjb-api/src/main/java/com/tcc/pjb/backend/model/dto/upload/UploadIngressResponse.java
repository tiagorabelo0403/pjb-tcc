package com.tcc.pjb.backend.model.dto.upload;

public record UploadIngressResponse(
        String status,
        String sha256,
        String sha384,
        String storageUri
) {
}
