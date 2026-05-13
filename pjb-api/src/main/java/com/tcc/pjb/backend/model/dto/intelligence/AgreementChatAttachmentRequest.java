package com.tcc.pjb.backend.model.dto.intelligence;

public record AgreementChatAttachmentRequest(
        Long propostaId,
        String kind,
        String label,
        String url,
        String mimeType,
        String hash,
        Long bytes
) {
}
