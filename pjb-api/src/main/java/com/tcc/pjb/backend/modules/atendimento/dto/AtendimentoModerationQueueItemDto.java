package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoModerationQueueItemDto(
    Long messageId,
    Long threadId,
    Long processoId,
    String processoNumero,
    Long senderUserId,
    String senderTipo,
    String status,
    Instant createdAt,
    String blockedReason,
    Instant blockedAt,
    int attachmentTotal,
    int attachmentReady,
    int attachmentPending,
    int attachmentRejected
) {
}
