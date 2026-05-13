package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsGranularityStatusResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsMaterializationResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsOperationalStatusResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PjbAuthorizationTrailAnalyticsService {

    private final PjbAuthorizationTrailAnalyticsRepository repository;
    private final PjbAuthorizationTrailReadModelService readModelService;
    private final PjbAuthorizationTrailAnalyticsMaterializationService materializationService;
    private final PjbAuthorizationTrailAnalyticsIncrementalRefreshService incrementalRefreshService;
    private final PjbAuthorizationTrailAnalyticsRefreshQueueService refreshQueueService;
    private final PjbAuthorizationTrailAnalyticsProjectionAssembler projectionAssembler;
    private final PjbAuthorizationTrailAnalyticsMaterializationAssembler materializationAssembler;

    public PjbAuthorizationTrailAnalyticsService(PjbAuthorizationTrailAnalyticsRepository repository,
                                                 PjbAuthorizationTrailReadModelService readModelService,
                                                 PjbAuthorizationTrailAnalyticsMaterializationService materializationService,
                                                 PjbAuthorizationTrailAnalyticsIncrementalRefreshService incrementalRefreshService,
                                                 PjbAuthorizationTrailAnalyticsRefreshQueueService refreshQueueService) {
        this.repository = repository;
        this.readModelService = readModelService;
        this.materializationService = materializationService;
        this.incrementalRefreshService = incrementalRefreshService;
        this.refreshQueueService = refreshQueueService;
        this.projectionAssembler = new PjbAuthorizationTrailAnalyticsProjectionAssembler();
        this.materializationAssembler = new PjbAuthorizationTrailAnalyticsMaterializationAssembler();
    }

    public PjbAuthorizationTrailAnalyticsMaterializationResponse materialize(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                             Instant occurredAfter,
                                                                             Instant occurredBefore,
                                                                             Integer limit,
                                                                             Boolean replaceExisting) {
        return materializationService.materialize(granularity, occurredAfter, occurredBefore, limit, replaceExisting);
    }

    public PjbAuthorizationTrailAnalyticsMaterializationResponse refreshBucket(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                               Instant bucketAt) {
        return incrementalRefreshService.refreshBucket(granularity, bucketAt);
    }

    public PjbAuthorizationTrailAnalyticsOperationalStatusResponse status() {
        PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse queueStatus = refreshQueueService.status();
        return new PjbAuthorizationTrailAnalyticsOperationalStatusResponse(
                queueStatus.workerEnabled(),
                readModelService.totalEntries(),
                readModelService.oldestOccurredAt(),
                readModelService.newestOccurredAt(),
                List.of(
                        toGranularityStatus(PjbAuthorizationTrailTemporalGranularity.HOUR),
                        toGranularityStatus(PjbAuthorizationTrailTemporalGranularity.DAY)
                ),
                queueStatus
        );
    }

    public PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse queueStatus() {
        return refreshQueueService.status();
    }

    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse drainQueue(Integer limit) {
        return refreshQueueService.drain(limit);
    }

    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse backfillQueue(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                                     Instant occurredAfter,
                                                                                     Instant occurredBefore) {
        return refreshQueueService.enqueueWindow(granularity, occurredAfter, occurredBefore);
    }

    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse recoverStaleQueue() {
        return refreshQueueService.recoverStaleProcessing();
    }

    public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse cleanupCompletedQueue() {
        return refreshQueueService.cleanupCompleted();
    }

    public PjbAuthorizationTrailAnalyticsResponse analytics(PjbAuthorizationTrailTemporalGranularity granularity,
                                                            Instant occurredAfter,
                                                            Instant occurredBefore,
                                                            Integer topN,
                                                            Boolean materializeOnRead,
                                                            Integer limit) {
        PjbAuthorizationTrailTemporalGranularity effectiveGranularity = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY : granularity;
        Instant effectiveAfter = occurredAfter == null ? Instant.now().minus(30, ChronoUnit.DAYS) : occurredAfter;
        Instant effectiveBefore = occurredBefore == null ? Instant.now() : occurredBefore;
        if (!effectiveBefore.isAfter(effectiveAfter)) {
            effectiveBefore = effectiveAfter.plus(1, effectiveGranularity == PjbAuthorizationTrailTemporalGranularity.HOUR ? ChronoUnit.HOURS : ChronoUnit.DAYS);
        }
        if (Boolean.TRUE.equals(materializeOnRead)) {
            materializationService.materialize(effectiveGranularity, effectiveAfter, effectiveBefore, limit, Boolean.TRUE);
        }
        List<PjbAuthorizationTrailAnalyticsEntry> entries = repository
                .findByGranularityAndBucketStartedAtGreaterThanEqualAndBucketStartedAtLessThanOrderByBucketStartedAtAscDimensionTypeAscTotalCountDesc(
                        effectiveGranularity.name(),
                        toLocalDateTime(materializationAssembler.bucketStart(effectiveAfter, effectiveGranularity)),
                        toLocalDateTime(materializationAssembler.bucketEnd(materializationAssembler.bucketStart(effectiveBefore, effectiveGranularity), effectiveGranularity))
                );
        return projectionAssembler.assemble(effectiveGranularity, topN == null ? 10 : topN, entries);
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private PjbAuthorizationTrailAnalyticsGranularityStatusResponse toGranularityStatus(PjbAuthorizationTrailTemporalGranularity granularity) {
        String code = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY.name() : granularity.name();
        PjbAuthorizationTrailAnalyticsEntry oldest = repository.findFirstByGranularityOrderByBucketStartedAtAscDimensionTypeAsc(code);
        PjbAuthorizationTrailAnalyticsEntry newest = repository.findFirstByGranularityOrderByBucketStartedAtDescDimensionTypeAsc(code);
        return new PjbAuthorizationTrailAnalyticsGranularityStatusResponse(
                code,
                repository.countByGranularity(code),
                oldest == null ? null : oldest.bucketStartedAtInstant(),
                newest == null ? null : newest.bucketStartedAtInstant()
        );
    }
}
