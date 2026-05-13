package com.tcc.pjb.backend.modules.atendimento.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.tcc.pjb.backend.modules.atendimento.model.ChecklistThreadAgg;

class ChecklistBadgeUtilsTest {

  @Test
  void computeNextDueInMinutes_nullWhenNoNextDue() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");
    assertThat(ChecklistBadgeUtils.computeNextDueInMinutes(now, null)).isNull();
  }

  @Test
  void computeNextDueInMinutes_clampsToZeroWhenPast() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");
    Instant past = now.minusSeconds(120);
    assertThat(ChecklistBadgeUtils.computeNextDueInMinutes(now, past)).isEqualTo(0L);
  }

  @Test
  void computeOverdueSinceMinutes_nullWhenNoOverdue() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");
    ChecklistThreadAgg agg = new ChecklistThreadAgg(0, 0, null, null);
    assertThat(ChecklistBadgeUtils.computeOverdueSinceMinutes(now, agg)).isNull();
  }

  @Test
  void computeOverdueSinceMinutes_positive() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");
    ChecklistThreadAgg agg = new ChecklistThreadAgg(1, 1, null, now.minusSeconds(61));
    assertThat(ChecklistBadgeUtils.computeOverdueSinceMinutes(now, agg)).isEqualTo(1L);
  }
}
