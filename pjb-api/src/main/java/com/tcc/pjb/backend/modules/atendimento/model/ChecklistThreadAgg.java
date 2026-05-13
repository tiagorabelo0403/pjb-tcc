package com.tcc.pjb.backend.modules.atendimento.model;

import java.time.Instant;







public record ChecklistThreadAgg(
    int openCount,
    int overdueCount,
    Instant nextDueAt,
    Instant oldestOverdueAt
) {
  public static ChecklistThreadAgg empty() {
    return new ChecklistThreadAgg(0, 0, null, null);
  }
}
