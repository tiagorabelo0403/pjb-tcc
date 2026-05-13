package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;
import java.util.List;

public record AtendimentoModerationMessageDetailDto(
    Long messageId,
    Long threadId,
    Long processoId,
    String processoNumero,
    Long senderUserId,
    String senderTipo,
    String status,
    String body,
    Instant createdAt,
    String msgHash,
    String prevHash,
    String blockedReason,
    String blockedNote,
    Instant blockedAt,
    Long blockedByUserId,
    List<AtendimentoAttachmentDto> attachments
) {
}
