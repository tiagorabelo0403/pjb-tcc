package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoCreateReminderRequest(
    String body,
    Instant fireAt,
    Long targetUserId
) {
}
