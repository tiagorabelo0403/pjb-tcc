package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;




public record AtendimentoMessageReplyPreviewDto(
    Long messageId,
    Long senderUsuarioId,
    String senderTipo,
    String bodyPreview,
    Instant createdAt,
    String senderDisplayName,
    String senderLabel
) {
}
