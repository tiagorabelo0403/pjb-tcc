package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoUpdateThreadNotificationSettingsRequest(
    Instant mutedUntil,
    Integer quietHoursStartMin,
    Integer quietHoursEndMin,
    Integer quietDaysMask
) {
}
