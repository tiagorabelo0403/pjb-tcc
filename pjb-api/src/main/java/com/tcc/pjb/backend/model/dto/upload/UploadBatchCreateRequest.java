package com.tcc.pjb.backend.model.dto.upload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UploadBatchCreateRequest(
        @NotNull Long processoId,
        @Min(1) Integer expectedCount
) {
}
