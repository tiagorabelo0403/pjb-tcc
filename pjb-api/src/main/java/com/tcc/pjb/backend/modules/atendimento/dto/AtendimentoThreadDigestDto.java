package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoThreadDigestDto(
    Long threadId,
    Long processoId,
    Long viewerUserId,
    boolean viewerHasUnread,
    Long lastMessageId,
    Instant lastActivityAt,
    String lastMessagePreview,
    boolean citizenSendDisabledNow,
    Instant cidadaoSendDisabledUntil,
    boolean notificationsMutedNow,
    Instant mutedUntil,
    boolean attachmentsEnabled,
    long attachmentMaxBytes,
    int attachmentMaxPerMessage,
    int openChecklistCount,
    int overdueChecklistCount,
    Instant nextChecklistDueAt,
    Long nextChecklistDueInMinutes,
    Long overdueSinceMinutes,
    Instant serverTime
) {
}
