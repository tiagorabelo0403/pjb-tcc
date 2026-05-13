package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.LocalDateTime;

public record AgreementChatAttachmentResponse(
        String kind,
        String label,
        String url,
        String mimeType,
        String hash,
        Long bytes,
        LocalDateTime registeredAt,
        String registeredBy
) {
}
