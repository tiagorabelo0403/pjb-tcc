package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;
import java.util.List;








public record AtendimentoMessageDto(
    Long id,
    Long threadId,
    Long senderUsuarioId,
    String senderTipo,
    String status,
    String body,
    Instant createdAt,
    List<AtendimentoAttachmentDto> attachments,
    Long replyToMessageId,
    AtendimentoMessageReplyPreviewDto replyTo,
    boolean deliveredToOther,
    boolean readByOther,
    Instant deliveredAtByOther,
    Instant readAtByOther,
    String senderDisplayName,
    String senderLabel,
    String senderOab
) {
}
