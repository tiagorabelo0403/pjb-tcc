package com.tcc.pjb.backend.modules.atendimento.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistItem;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemKind;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;

@DataJpaTest
@ActiveProfiles("test")
class AtendimentoChecklistItemInvariantTest {

  @Autowired
  AtendimentoChecklistItemRepository repo;

  @Test
  void open_cannot_have_completed_fields() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");

    AtendimentoChecklistItem bad = AtendimentoChecklistItem.builder()
        .threadId(1L)
        .kind(AtendimentoChecklistItemKind.OUTRO)
        .status(AtendimentoChecklistItemStatus.OPEN)
        .title("Tarefa")
        .createdByUserId(1L)
        .createdAt(now)
        .updatedAt(now)
        .completedAt(now)
        .completedByUserId(1L)
        .build();

    assertThatThrownBy(() -> repo.saveAndFlush(bad))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void done_requires_completed_fields() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");

    AtendimentoChecklistItem bad = AtendimentoChecklistItem.builder()
        .threadId(1L)
        .kind(AtendimentoChecklistItemKind.OUTRO)
        .status(AtendimentoChecklistItemStatus.DONE)
        .title("Tarefa")
        .createdByUserId(1L)
        .createdAt(now)
        .updatedAt(now)
        .build();

    assertThatThrownBy(() -> repo.saveAndFlush(bad))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void cancelled_requires_cancel_fields_and_cannot_have_completed_fields() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");

    AtendimentoChecklistItem bad = AtendimentoChecklistItem.builder()
        .threadId(1L)
        .kind(AtendimentoChecklistItemKind.OUTRO)
        .status(AtendimentoChecklistItemStatus.CANCELLED)
        .title("Tarefa")
        .createdByUserId(1L)
        .createdAt(now)
        .updatedAt(now)
        .cancelledAt(now)
        .cancelledByUserId(1L)
        .completedAt(now)
        .completedByUserId(1L)
        .build();

    assertThatThrownBy(() -> repo.saveAndFlush(bad))
        .isInstanceOf(RuntimeException.class);
  }
}
