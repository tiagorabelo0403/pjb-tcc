package com.tcc.pjb.backend.model.dto.upload;

import java.util.List;
import java.util.UUID;

public record UploadBatchFinalizeResponse(
        UUID batchId,
        int documentosCriados,
        List<UUID> documentoIds
) {
}
