package com.tcc.pjb.backend.repository.secretariat;

import java.util.Collection;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;

public interface SecretariatQueueItemRepository extends JpaRepository<SecretariatQueueItem, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select q from SecretariatQueueItem q where q.workItemId = :workItemId")
  java.util.Optional<SecretariatQueueItem> findLockedByWorkItemId(@Param("workItemId") Long workItemId);


  @Query("""
      select q from SecretariatQueueItem q
      where q.inboxKey = :inboxKey
        and q.status in :statuses
      order by case when q.dueAt is null then 1 else 0 end, q.dueAt asc, q.prioridade asc, q.score desc, q.updatedAt desc
      """)
  Page<SecretariatQueueItem> listInbox(
      @Param("inboxKey") String inboxKey,
      @Param("statuses") Collection<String> statuses,
      Pageable pageable
  );

  @Query("select max(q.updatedAt), count(q) from SecretariatQueueItem q where q.inboxKey = :inboxKey and q.status in :statuses")
  Object[] signature(
      @Param("inboxKey") String inboxKey,
      @Param("statuses") List<String> statuses
  );

  @Query("""
      select count(q),
             coalesce(sum(case when q.dueAt is not null and q.dueAt < :now then 1 else 0 end), 0),
             coalesce(sum(case when q.prioridade is not null and q.prioridade <= 1 then 1 else 0 end), 0),
             coalesce(sum(case when q.dueAt is not null and q.dueAt >= :now and q.dueAt <= :limit then 1 else 0 end), 0)
      from SecretariatQueueItem q
      where q.inboxKey = :inboxKey
        and q.status in :statuses
      """)
  Object[] loadSignature(
      @Param("inboxKey") String inboxKey,
      @Param("statuses") Collection<String> statuses,
      @Param("now") java.time.Instant now,
      @Param("limit") java.time.Instant limit
  );

  @Query("""
      select count(q),
             coalesce(sum(case when q.dueAt is not null and q.dueAt < :now then 1 else 0 end), 0),
             coalesce(sum(case when q.prioridade is not null and q.prioridade <= 2 then 1 else 0 end), 0)
      from SecretariatQueueItem q
      where q.inboxKey = :inboxKey
        and q.status in :statuses
      """)
  Object[] workload(
      @Param("inboxKey") String inboxKey,
      @Param("statuses") Collection<String> statuses,
      @Param("now") java.time.Instant now
  );



  @Query("""
      select count(q),
             coalesce(sum(case when q.dueAt is not null and q.dueAt < :now then 1 else 0 end), 0),
             coalesce(sum(case when q.prioridade is not null and q.prioridade <= 2 then 1 else 0 end), 0)
      from SecretariatQueueItem q
      where q.inboxKey = :inboxKey
        and q.queueCode = :queueCode
        and q.status in :statuses
      """)
  Object[] workloadByInboxAndQueue(
      @Param("inboxKey") String inboxKey,
      @Param("queueCode") String queueCode,
      @Param("statuses") Collection<String> statuses,
      @Param("now") java.time.Instant now
  );
  @Query("""
      select coalesce(q.deskAxis, 'BASE'),
             count(q),
             coalesce(sum(case when q.dueAt is not null and q.dueAt < :now then 1 else 0 end), 0),
             coalesce(sum(case when q.blocking = true then 1 else 0 end), 0),
             coalesce(sum(case when q.secrecyReviewRequired = true then 1 else 0 end), 0),
             coalesce(sum(case when q.hearingSensitive = true then 1 else 0 end), 0)
      from SecretariatQueueItem q
      where q.inboxKey = :inboxKey
        and q.status in :statuses
      group by coalesce(q.deskAxis, 'BASE')
      """)
  List<Object[]> deskWorkload(
      @Param("inboxKey") String inboxKey,
      @Param("statuses") Collection<String> statuses,
      @Param("now") java.time.Instant now
  );


  @Query("""
      select q from SecretariatQueueItem q
      where q.inboxKey in :inboxKeys
        and q.status in :statuses
        and coalesce(q.dueAt, q.updatedAt, q.createdAt) between :from and :to
      order by case when q.dueAt is null then 1 else 0 end,
               coalesce(q.dueAt, q.updatedAt, q.createdAt) asc,
               q.prioridade asc,
               q.updatedAt desc,
               q.workItemId asc
      """)
  List<SecretariatQueueItem> findCalendarWindowByInboxKeys(
      @Param("inboxKeys") Collection<String> inboxKeys,
      @Param("statuses") Collection<String> statuses,
      @Param("from") java.time.Instant from,
      @Param("to") java.time.Instant to,
      Pageable pageable
  );

}
