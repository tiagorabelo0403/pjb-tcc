package com.tcc.pjb.backend.modules.atendimento.repository;

import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistItem;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;

public interface AtendimentoChecklistItemRepository extends JpaRepository<AtendimentoChecklistItem, Long> {

  Page<AtendimentoChecklistItem> findByThreadIdOrderByIdDesc(Long threadId, Pageable pageable);

  List<AtendimentoChecklistItem> findByThreadIdOrderByIdAsc(Long threadId);

  long countByThreadIdAndStatus(Long threadId, AtendimentoChecklistItemStatus status);


  interface ThreadCount {
    Long getThreadId();
    long getCnt();
  }

  @Query("select i.threadId as threadId, count(i) as cnt from AtendimentoChecklistItem i "
      + "where i.threadId in :threadIds and i.status = :status group by i.threadId")
  List<ThreadCount> countByThreadIdsAndStatus(@Param("threadIds") List<Long> threadIds,
                                             @Param("status") AtendimentoChecklistItemStatus status);


  interface ThreadChecklistAgg {
    Long getThreadId();
    long getOpenCnt();
    long getOverdueCnt();
    Instant getNextDueAt();
    Instant getOldestOverdueAt();
  }

  






  @Query("select i.threadId as threadId, "
      + "count(i) as openCnt, "
      + "sum(case when i.dueAt is not null and i.dueAt < :now then 1 else 0 end) as overdueCnt, "
      + "min(case when i.dueAt is not null and i.dueAt >= :now then i.dueAt else null end) as nextDueAt, "
      + "min(case when i.dueAt is not null and i.dueAt < :now then i.dueAt else null end) as oldestOverdueAt "
      + "from AtendimentoChecklistItem i where i.threadId in :threadIds and i.status = :openStatus group by i.threadId")
  List<ThreadChecklistAgg> aggregateByThreadIds(@Param("threadIds") List<Long> threadIds,
                                               @Param("openStatus") AtendimentoChecklistItemStatus openStatus,
                                               @Param("now") Instant now);

}