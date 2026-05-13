package com.tcc.pjb.backend.model.dto.upload;

import java.util.UUID;

public record UploadBatchCreateResponse(
        UUID batchId,
        String status
) {
}
