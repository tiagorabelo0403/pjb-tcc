package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;







public record AtendimentoThreadDto(
    Long threadId,
    Long processoId,
    String numeroUnificado,
    String titulo,
    String otherParty,
    Instant lastActivityAt,
    String lastMessagePreview,
    boolean hasUnread,
    String status,
    boolean mutedNow,
    Instant mutedUntil,
    int openChecklistCount,
    int overdueChecklistCount,
    Instant nextChecklistDueAt,
    Long nextChecklistDueInMinutes,
    Long overdueSinceMinutes,
    Long otherPartyUsuarioId,
    String otherPartyTipo,
    String otherPartyOab,
    String otherPartyLabel
) {
}
