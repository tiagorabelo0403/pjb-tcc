package com.tcc.pjb.backend.modules.atendimento.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AtendimentoMessageRepository extends JpaRepository<AtendimentoMessage, Long> {

  Page<AtendimentoMessage> findByThreadIdAndIdGreaterThanOrderByIdAsc(Long threadId, Long afterId, Pageable pageable);

  Page<AtendimentoMessage> findByThreadIdAndIdLessThanOrderByIdDesc(Long threadId, Long beforeId, Pageable pageable);

  Page<AtendimentoMessage> findByThreadIdOrderByIdDesc(Long threadId, Pageable pageable);

  List<AtendimentoMessage> findByThreadIdOrderByIdAsc(Long threadId);

  List<AtendimentoMessage> findByThreadIdAndCreatedAtBetweenOrderByIdAsc(Long threadId, Instant from, Instant to);

  Optional<AtendimentoMessage> findTopByThreadIdOrderByIdDesc(Long threadId);

  Optional<AtendimentoMessage> findByThreadIdAndClientMessageId(Long threadId, UUID clientMessageId);

  List<AtendimentoMessage> findTop200ByStatusOrderByIdAsc(AtendimentoMessageStatus status);

  @Query(
      "select m from AtendimentoMessage m where m.status in :statuses and (:cursor is null or m.id < :cursor) order by m.id desc"
  )
  Page<AtendimentoMessage> findQueue(@Param("statuses") List<AtendimentoMessageStatus> statuses, @Param("cursor") Long cursor, Pageable pageable);
}
