package com.tcc.pjb.backend.repository.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  @Query(
      value = "select id from tb_outbox_event "
          + "where status = :status and available_at <= now() "
          + "order by created_at asc "
          + "limit :limit "
          + "for update skip locked",
      nativeQuery = true)
  List<UUID> claimIdsForUpdate(
      @Param("status") String status,
      @Param("limit") int limit);

  @Query(
      value = "select id from tb_outbox_event where status = :status and available_at <= :now order by created_at asc limit :limit",
      nativeQuery = true)
  List<UUID> peekIds(
      @Param("status") String status,
      @Param("now") Instant now,
      @Param("limit") int limit);

  java.util.List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(String aggregateType, String aggregateId);

  long countByStatus(OutboxStatus status);

  @Query("select min(e.createdAt) from OutboxEvent e where e.status = :status")
  Instant findOldestCreatedAtByStatus(@Param("status") OutboxStatus status);

  @Query("select min(e.lockedAt) from OutboxEvent e where e.status = :status")
  Instant findOldestLockedAtByStatus(@Param("status") OutboxStatus status);
}
