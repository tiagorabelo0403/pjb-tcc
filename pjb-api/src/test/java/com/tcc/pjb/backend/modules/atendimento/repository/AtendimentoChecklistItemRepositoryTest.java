package com.tcc.pjb.backend.modules.atendimento.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistItem;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemKind;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AtendimentoChecklistItemRepositoryTest {

  @Autowired
  AtendimentoChecklistItemRepository repo;

  @Test
  void aggregateByThreadIds_countsOnlyOpen_andComputesNextAndOverdue() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");

    
    save(10L, AtendimentoChecklistItemStatus.OPEN, now.minus(2, ChronoUnit.HOURS));
    save(10L, AtendimentoChecklistItemStatus.OPEN, now.minus(1, ChronoUnit.HOURS));
    save(10L, AtendimentoChecklistItemStatus.OPEN, now.plus(45, ChronoUnit.MINUTES));
    save(10L, AtendimentoChecklistItemStatus.DONE, now.plus(5, ChronoUnit.HOURS));
    save(10L, AtendimentoChecklistItemStatus.CANCELLED, now.minus(10, ChronoUnit.HOURS));

    
    save(11L, AtendimentoChecklistItemStatus.OPEN, null);
    save(11L, AtendimentoChecklistItemStatus.DONE, now.minus(1, ChronoUnit.DAYS));

    
    save(12L, AtendimentoChecklistItemStatus.DONE, now.plus(1, ChronoUnit.DAYS));
    save(12L, AtendimentoChecklistItemStatus.CANCELLED, now.plus(2, ChronoUnit.DAYS));

    List<AtendimentoChecklistItemRepository.ThreadChecklistAgg> rows =
        repo.aggregateByThreadIds(List.of(10L, 11L, 12L), AtendimentoChecklistItemStatus.OPEN, now);

    Map<Long, AtendimentoChecklistItemRepository.ThreadChecklistAgg> byThread = rows.stream()
        .collect(Collectors.toMap(AtendimentoChecklistItemRepository.ThreadChecklistAgg::getThreadId, x -> x));

    assertThat(byThread).containsKeys(10L, 11L);
    assertThat(byThread).doesNotContainKey(12L);

    var t10 = byThread.get(10L);
    assertThat(t10.getOpenCnt()).isEqualTo(3);
    assertThat(t10.getOverdueCnt()).isEqualTo(2);
    assertThat(t10.getNextDueAt()).isEqualTo(now.plus(45, ChronoUnit.MINUTES));
    assertThat(t10.getOldestOverdueAt()).isEqualTo(now.minus(2, ChronoUnit.HOURS));

    var t11 = byThread.get(11L);
    assertThat(t11.getOpenCnt()).isEqualTo(1);
    assertThat(t11.getOverdueCnt()).isEqualTo(0);
    assertThat(t11.getNextDueAt()).isNull();
    assertThat(t11.getOldestOverdueAt()).isNull();
  }

  @Test
  void aggregateByThreadIds_nextDue_isSmallestFuture_dueAt_onlyOpen() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");

    save(20L, AtendimentoChecklistItemStatus.OPEN, now.plus(2, ChronoUnit.HOURS));
    save(20L, AtendimentoChecklistItemStatus.OPEN, now.plus(30, ChronoUnit.MINUTES));
    save(20L, AtendimentoChecklistItemStatus.OPEN, now.plus(90, ChronoUnit.MINUTES));

    
    save(20L, AtendimentoChecklistItemStatus.DONE, now.plus(10, ChronoUnit.MINUTES));

    var row = repo.aggregateByThreadIds(List.of(20L), AtendimentoChecklistItemStatus.OPEN, now).get(0);
    assertThat(row.getNextDueAt()).isEqualTo(now.plus(30, ChronoUnit.MINUTES));
  }

    private void save(Long threadId, AtendimentoChecklistItemStatus status, Instant dueAt) {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");

    AtendimentoChecklistItem.AtendimentoChecklistItemBuilder b = AtendimentoChecklistItem.builder()
        .threadId(threadId)
        .kind(AtendimentoChecklistItemKind.DOCUMENTO)
        .status(status)
        .title("Tarefa")
        .note(null)
        .dueAt(dueAt)
        .documentoId(null)
        .createdByUserId(1L)
        .createdAt(now)
        .updatedAt(now);

    if (status == AtendimentoChecklistItemStatus.DONE) {
      b.completedAt(now).completedByUserId(1L);
    } else if (status == AtendimentoChecklistItemStatus.CANCELLED) {
      b.cancelledAt(now).cancelledByUserId(1L);
    }

    repo.save(b.build());
  }
}

