package com.tcc.pjb.backend.core.security.abac;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PjbAuthorizationTrailAnalyticsRefreshQueueRepository extends JpaRepository<PjbAuthorizationTrailAnalyticsRefreshQueueEntry, Long> {

    Optional<PjbAuthorizationTrailAnalyticsRefreshQueueEntry> findByDedupKey(String dedupKey);

    long countByStatus(String status);

    long countByStatusAndLockedAtLessThanEqual(String status, LocalDateTime lockedAt);

    long countByStatusAndLastProcessedAtLessThanEqual(String status, LocalDateTime lastProcessedAt);

    PjbAuthorizationTrailAnalyticsRefreshQueueEntry findFirstByStatusOrderByBucketStartedAtAscIdAsc(String status);

    PjbAuthorizationTrailAnalyticsRefreshQueueEntry findFirstByStatusOrderByLastProcessedAtDescIdDesc(String status);

    List<PjbAuthorizationTrailAnalyticsRefreshQueueEntry> findByStatusInAndNextVisibleAtLessThanEqualOrderByBucketStartedAtAscIdAsc(
            Collection<String> statuses,
            LocalDateTime nextVisibleAt,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update PjbAuthorizationTrailAnalyticsRefreshQueueEntry e
               set e.status = :processingStatus,
                   e.lockedAt = :now,
                   e.nextVisibleAt = :now,
                   e.attemptCount = e.attemptCount + 1
             where e.id = :id
               and e.status in :claimableStatuses
               and e.nextVisibleAt <= :now
            """)
    int claimForProcessing(@Param("id") Long id,
                           @Param("claimableStatuses") Collection<String> claimableStatuses,
                           @Param("processingStatus") String processingStatus,
                           @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update PjbAuthorizationTrailAnalyticsRefreshQueueEntry e
               set e.status = :failedStatus,
                   e.lockedAt = null,
                   e.lastProcessedAt = :now,
                   e.nextVisibleAt = :now,
                   e.requeueRequested = false,
                   e.lastError = :error
             where e.status = :processingStatus
               and e.lockedAt is not null
               and e.lockedAt <= :cutoff
            """)
    int recoverStaleProcessing(@Param("processingStatus") String processingStatus,
                               @Param("failedStatus") String failedStatus,
                               @Param("cutoff") LocalDateTime cutoff,
                               @Param("now") LocalDateTime now,
                               @Param("error") String error);

    @Modifying
    @Query("""
            delete from PjbAuthorizationTrailAnalyticsRefreshQueueEntry e
             where e.status = :completedStatus
               and e.lastProcessedAt is not null
               and e.lastProcessedAt <= :cutoff
            """)
    int deleteCompletedBefore(@Param("completedStatus") String completedStatus,
                              @Param("cutoff") LocalDateTime cutoff);
}
