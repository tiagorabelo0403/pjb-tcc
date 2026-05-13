package com.tcc.pjb.backend.model.dto.upload;

import java.util.UUID;

public record UploadItemReserveResponse(
        UUID itemId,
        String uploadUrl,
        String status
) {
}
