package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbAuthorizationTrailAnalyticsRefreshQueueService {

    private final PjbAuthorizationTrailAnalyticsRefreshQueueRepository repository;
    private final PjbAuthorizationTrailAnalyticsIncrementalRefreshService incrementalRefreshService;
    private final PjbAuthorizationTrailAnalyticsRefreshProperties properties;
    private final PjbAuthorizationTrailAnalyticsMaterializationAssembler materializationAssembler;

    public PjbAuthorizationTrailAnalyticsRefreshQueueService(PjbAuthorizationTrailAnalyticsRefreshQueueRepository repository,
                                                             PjbAuthorizationTrailAnalyticsIncrementalRefreshService incrementalRefreshService,
                                                             PjbAuthorizationTrailAnalyticsRefreshProperties properties) {
        this.repository = repository;
        this.incrementalRefreshService = incrementalRefreshService;
        this.properties = properties;
        this.materializationAssembler = new PjbAuthorizationTrailAnalyticsMaterializationAssembler();
    }

    @Transactional
    public void enqueueSnapshot(PjbAuthorizationTrailSnapshot snapshot) {
        if (snapshot == null || snapshot.occurredAt() == null) {
            return;
        }
        enqueueBucket(PjbAuthorizationTrailTemporalGranularity.HOUR, snapshot.occurredAt());
        enqueueBucket(PjbAuthorizationTrailTemporalGranularity.DAY, snapshot.occurredAt());
    }

    @Transactional
    public void enqueueBucket(PjbAuthorizationTrailTemporalGranularity granularity, Instant bucketReference) {
        PjbAuthorizationTrailTemporalGranularity effectiveGranularity = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY : granularity;
        Instant reference = bucketReference == null ? Instant.now() : bucketReference;
        Instant bucketStartedAt = materializationAssembler.bucketStart(reference, effectiveGranularity);
        Instant bucketEndedAtExclusive = materializationAssembler.bucketEnd(bucketStartedAt, effectiveGranularity);
        Instant now = Instant.now();
        String dedupKey = effectiveGranularity.name() + '|' + toLocalDateTime(bucketStartedAt);
        PjbAuthorizationTrailAnalyticsRefreshQueueEntry existing = repository.findByDedupKey(dedupKey).orElse(null);
        if (existing == null) {
            try {
                repository.save(PjbAuthorizationTrailAnalyticsRefreshQueueEntry.of(effectiveGranularity, bucketStartedAt, bucketEndedAtExclusive, now));
                return;
            } catch (DataIntegrityViolationException ex) {
                existing = repository.findByDedupKey(dedupKey).orElse(null);
            }
        }
        if (existing == null) {
            return;
        }
        existing.markEnqueued(now);
        repository.save(existing);
    }

    @Transactional
    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse enqueueWindow(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                                     Instant occurredAfter,
                                                                                     Instant occurredBefore) {
        Instant now = Instant.now();
        Instant effectiveAfter = occurredAfter == null ? now.minusSeconds(86400) : occurredAfter;
        Instant effectiveBefore = occurredBefore == null ? now : occurredBefore;
        if (!effectiveBefore.isAfter(effectiveAfter)) {
            effectiveBefore = effectiveAfter.plusSeconds(3600);
        }
        int maxBuckets = Math.max(24, properties.getMaxBackfillBuckets());
        int touched;
        if (granularity == null) {
            touched = enqueueWindowForGranularity(PjbAuthorizationTrailTemporalGranularity.HOUR, effectiveAfter, effectiveBefore, maxBuckets);
            touched += enqueueWindowForGranularity(PjbAuthorizationTrailTemporalGranularity.DAY, effectiveAfter, effectiveBefore, maxBuckets);
            return new PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse(
                    "BACKFILL",
                    "ALL",
                    effectiveAfter,
                    effectiveBefore,
                    touched,
                    0,
                    0,
                    pendingBuckets(),
                    now
            );
        }
        touched = enqueueWindowForGranularity(granularity, effectiveAfter, effectiveBefore, maxBuckets);
        return new PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse(
                "BACKFILL",
                granularity.name(),
                effectiveAfter,
                effectiveBefore,
                touched,
                0,
                0,
                pendingBuckets(),
                now
        );
    }

    @Transactional
    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse drain(Integer limit) {
        Instant now = Instant.now();
        recoverStaleProcessing(now);
        int effectiveLimit = Math.max(1, Math.min(512, limit == null ? properties.getBatchSize() : limit));
        List<PjbAuthorizationTrailAnalyticsRefreshQueueEntry> candidates = repository
                .findByStatusInAndNextVisibleAtLessThanEqualOrderByBucketStartedAtAscIdAsc(
                        List.of(
                                PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name(),
                                PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name()
                        ),
                        toLocalDateTime(now),
                        PageRequest.of(0, effectiveLimit)
                );
        int touched = 0;
        int processed = 0;
        int failed = 0;
        for (PjbAuthorizationTrailAnalyticsRefreshQueueEntry candidate : candidates) {
            if (!claim(candidate.getId(), now)) {
                continue;
            }
            touched++;
            PjbAuthorizationTrailAnalyticsRefreshQueueEntry locked = repository.findById(candidate.getId()).orElse(null);
            if (locked == null) {
                continue;
            }
            try {
                incrementalRefreshService.refreshBucket(PjbAuthorizationTrailTemporalGranularity.valueOf(locked.getGranularity()), locked.bucketStartedAtInstant());
                locked.markCompleted(Instant.now());
                repository.save(locked);
                processed++;
            } catch (RuntimeException ex) {
                locked.markFailed(Instant.now(), ex.getMessage());
                repository.save(locked);
                failed++;
            }
        }
        return new PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse(
                "DRAIN",
                "QUEUE",
                null,
                null,
                touched,
                processed,
                failed,
                pendingBuckets(),
                now
        );
    }

    @Transactional(readOnly = true)
    public PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse status() {
        LocalDateTime staleCutoff = staleCutoff(Instant.now());
        LocalDateTime cleanupCutoff = cleanupCutoff(Instant.now());
        PjbAuthorizationTrailAnalyticsRefreshQueueEntry oldestPending = repository.findFirstByStatusOrderByBucketStartedAtAscIdAsc(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name());
        PjbAuthorizationTrailAnalyticsRefreshQueueEntry newestCompleted = repository.findFirstByStatusOrderByLastProcessedAtDescIdDesc(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.COMPLETED.name());
        PjbAuthorizationTrailAnalyticsRefreshQueueEntry newestFailure = repository.findFirstByStatusOrderByLastProcessedAtDescIdDesc(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name());
        return new PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse(
                properties.isEnabled(),
                Math.max(1, properties.getBatchSize()),
                Math.max(1, Math.toIntExact(Math.max(1L, properties.getProcessingTimeoutMs()) / 1000L)),
                Math.max(1, properties.getCompletionRetentionDays()),
                toInt(repository.countByStatus(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name())),
                toInt(repository.countByStatus(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PROCESSING.name())),
                toInt(repository.countByStatus(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name())),
                toInt(repository.countByStatus(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.COMPLETED.name())),
                toInt(repository.countByStatusAndLockedAtLessThanEqual(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PROCESSING.name(), staleCutoff)),
                toInt(repository.countByStatusAndLastProcessedAtLessThanEqual(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.COMPLETED.name(), cleanupCutoff)),
                oldestPending == null ? null : oldestPending.bucketStartedAtInstant(),
                newestCompleted == null ? null : newestCompleted.lastProcessedAtInstant(),
                newestFailure == null ? null : newestFailure.lastProcessedAtInstant()
        );
    }

    @Transactional
    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse recoverStaleProcessing() {
        Instant now = Instant.now();
        int recovered = recoverStaleProcessing(now);
        return new PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse(
                "RECOVER_STALE",
                "QUEUE",
                null,
                null,
                recovered,
                recovered,
                0,
                pendingBuckets(),
                now
        );
    }

    @Transactional
    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse cleanupCompleted() {
        Instant now = Instant.now();
        int deleted = repository.deleteCompletedBefore(
                PjbAuthorizationTrailAnalyticsRefreshQueueStatus.COMPLETED.name(),
                cleanupCutoff(now)
        );
        return new PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse(
                "CLEANUP_COMPLETED",
                "QUEUE",
                null,
                null,
                deleted,
                deleted,
                0,
                pendingBuckets(),
                now
        );
    }

    private int enqueueWindowForGranularity(PjbAuthorizationTrailTemporalGranularity granularity,
                                            Instant occurredAfter,
                                            Instant occurredBefore,
                                            int maxBuckets) {
        Instant cursor = materializationAssembler.bucketStart(occurredAfter, granularity);
        int touched = 0;
        while (cursor.isBefore(occurredBefore) && touched < maxBuckets) {
            enqueueBucket(granularity, cursor);
            cursor = materializationAssembler.bucketEnd(cursor, granularity);
            touched++;
        }
        return touched;
    }

    private boolean claim(Long id, Instant now) {
        return repository.claimForProcessing(
                id,
                List.of(
                        PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name(),
                        PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name()
                ),
                PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PROCESSING.name(),
                toLocalDateTime(now)
        ) > 0;
    }

    private int recoverStaleProcessing(Instant now) {
        return repository.recoverStaleProcessing(
                PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PROCESSING.name(),
                PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name(),
                staleCutoff(now),
                toLocalDateTime(now),
                "STALE_PROCESSING_TIMEOUT"
        );
    }

    private int pendingBuckets() {
        long pending = repository.countByStatus(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name());
        long failed = repository.countByStatus(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name());
        long total = pending + failed;
        return toInt(total);
    }

    private static int toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private LocalDateTime staleCutoff(Instant now) {
        long timeoutMs = Math.max(1000L, properties.getProcessingTimeoutMs());
        return toLocalDateTime((now == null ? Instant.now() : now).minusMillis(timeoutMs));
    }

    private LocalDateTime cleanupCutoff(Instant now) {
        int retentionDays = Math.max(1, properties.getCompletionRetentionDays());
        return toLocalDateTime((now == null ? Instant.now() : now).minusSeconds(retentionDays * 86400L));
    }
}
