package com.tcc.pjb.backend.repository.ui;

import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.ui.UiStateHistory;

public interface UiStateHistoryRepository extends JpaRepository<UiStateHistory, UUID> {

  Page<UiStateHistory> findByProcessoIdOrderByOccurredAtDesc(Long processoId, Pageable pageable);

  Page<UiStateHistory> findByWorkItemIdOrderByOccurredAtDesc(Long workItemId, Pageable pageable);

  Page<UiStateHistory> findByInboxKeyOrderByOccurredAtDesc(String inboxKey, Pageable pageable);

  @Query("select max(h.occurredAt), count(h) from UiStateHistory h where h.processoId = :pid")
  Object[] signatureProcesso(@Param("pid") Long processoId);

  @Query("select max(h.occurredAt), count(h) from UiStateHistory h where h.workItemId = :wid")
  Object[] signatureWorkItem(@Param("wid") Long workItemId);

  @Query("select max(h.occurredAt), count(h) from UiStateHistory h where h.inboxKey = :inbox")
  Object[] signatureInbox(@Param("inbox") String inboxKey);

  @Modifying
  @org.springframework.transaction.annotation.Transactional
  @Query("delete from UiStateHistory h where h.occurredAt < :cutoff")
  int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
