package com.tcc.pjb.backend.model.dto.upload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UploadItemReserveRequest(
        @NotBlank String nomeOriginal,
        String contentType,
        @Min(1) long tamanhoBytes,
        @NotBlank String hashSha384,
        String edgeAttestationJson
) {
}
