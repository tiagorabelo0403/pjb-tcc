package com.tcc.pjb.backend.modules.atendimento.util;

import java.time.Duration;
import java.time.Instant;
import com.tcc.pjb.backend.modules.atendimento.model.ChecklistThreadAgg;







public final class ChecklistBadgeUtils {

  private ChecklistBadgeUtils() {
  }

  


  public static Long computeNextDueInMinutes(Instant now, Instant nextDueAt) {
    if (now == null || nextDueAt == null) return null;
    long minutes;
    try {
      minutes = Duration.between(now, nextDueAt).toMinutes();
    } catch (Exception ignored) {
      return null;
    }
    return minutes <= 0 ? 0L : minutes;
  }

  


  public static Long computeOverdueSinceMinutes(Instant now, ChecklistThreadAgg agg) {
    if (now == null || agg == null) return null;
    Instant oldest = agg.oldestOverdueAt();
    if (oldest == null) return null;

    long minutes;
    try {
      minutes = Duration.between(oldest, now).toMinutes();
    } catch (Exception ignored) {
      return null;
    }
    return minutes <= 0 ? 0L : minutes;
  }
}
