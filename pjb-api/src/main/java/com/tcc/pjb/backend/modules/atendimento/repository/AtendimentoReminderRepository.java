package com.tcc.pjb.backend.modules.atendimento.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminder;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminderStatus;

public interface AtendimentoReminderRepository extends JpaRepository<AtendimentoReminder, Long> {

  Page<AtendimentoReminder> findByThreadIdOrderByFireAtDesc(Long threadId, Pageable pageable);

  List<AtendimentoReminder> findByStatusAndFireAtLessThanEqualOrderByFireAtAsc(AtendimentoReminderStatus status, Instant fireAt, Pageable pageable);

  @Modifying
  @Query("update AtendimentoReminder r set r.status = com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminderStatus.SENDING, r.updatedAt = :now where r.id = :id and r.status = com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminderStatus.PENDING")
  int claim(@Param("id") Long id, @Param("now") Instant now);

  @Modifying
  @Query("update AtendimentoReminder r set r.status = com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminderStatus.CANCELED, r.updatedAt = :now where r.id = :id and r.status <> com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminderStatus.SENT")
  int cancelAnyNotSent(@Param("id") Long id, @Param("now") Instant now);
}
