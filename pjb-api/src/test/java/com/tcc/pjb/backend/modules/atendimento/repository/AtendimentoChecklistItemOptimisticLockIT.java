package com.tcc.pjb.backend.modules.atendimento.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistItem;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemKind;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;

@DataJpaTest
@ActiveProfiles("test")
class AtendimentoChecklistItemOptimisticLockIT {

  @Autowired
  AtendimentoChecklistItemRepository repo;

  @Autowired
  EntityManagerFactory emf;

  @Test
  void concurrent_updates_should_fail_with_optimistic_lock() {
    Instant now = Instant.parse("2026-02-28T12:00:00Z");

    AtendimentoChecklistItem base = repo.saveAndFlush(AtendimentoChecklistItem.builder()
        .threadId(1L)
        .kind(AtendimentoChecklistItemKind.OUTRO)
        .status(AtendimentoChecklistItemStatus.OPEN)
        .title("Tarefa")
        .createdByUserId(1L)
        .createdAt(now)
        .updatedAt(now)
        .build());

    EntityManager em1 = emf.createEntityManager();
    EntityManager em2 = emf.createEntityManager();

    try {
      var tx1 = em1.getTransaction();
      var tx2 = em2.getTransaction();

      tx1.begin();
      AtendimentoChecklistItem a = em1.find(AtendimentoChecklistItem.class, base.getId());

      tx2.begin();
      AtendimentoChecklistItem b = em2.find(AtendimentoChecklistItem.class, base.getId());

      
      b.setNote("tx2");
      b.setUpdatedAt(now.plusSeconds(10));
      em2.flush();
      tx2.commit();

      
      a.setNote("tx1");
      a.setUpdatedAt(now.plusSeconds(20));

      assertThatThrownBy(() -> {
        em1.flush();
        tx1.commit();
      }).isInstanceOfAny(OptimisticLockException.class, jakarta.persistence.RollbackException.class);

      if (tx1.isActive()) tx1.rollback();

    } finally {
      em1.close();
      em2.close();
    }
  }
}
