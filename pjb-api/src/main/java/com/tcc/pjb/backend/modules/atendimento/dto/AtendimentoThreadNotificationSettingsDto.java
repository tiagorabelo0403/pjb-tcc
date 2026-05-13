package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoThreadNotificationSettingsDto(
    Long threadId,
    Long usuarioId,
    Instant mutedUntil,
    Integer quietHoursStartMin,
    Integer quietHoursEndMin,
    Integer quietDaysMask,
    boolean notificationsMutedNow,
    Instant serverTime
) {
}
