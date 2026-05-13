package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoAttachmentDownloadDto(
    Long attachmentId,
    String url,
    Instant expiresAt
) {
}
