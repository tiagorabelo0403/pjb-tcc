package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsMaterializationResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbAuthorizationTrailAnalyticsMaterializationService {

    private final PjbAuthorizationTrailReadModelService readModelService;
    private final PjbAuthorizationTrailAnalyticsRepository analyticsRepository;
    private final PjbAuthorizationTrailAnalyticsMaterializationAssembler materializationAssembler;

    public PjbAuthorizationTrailAnalyticsMaterializationService(PjbAuthorizationTrailReadModelService readModelService,
                                                                PjbAuthorizationTrailAnalyticsRepository analyticsRepository) {
        this.readModelService = readModelService;
        this.analyticsRepository = analyticsRepository;
        this.materializationAssembler = new PjbAuthorizationTrailAnalyticsMaterializationAssembler();
    }

    @Transactional
    public PjbAuthorizationTrailAnalyticsMaterializationResponse materialize(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                            Instant occurredAfter,
                                                                            Instant occurredBefore,
                                                                            Integer limit,
                                                                            Boolean replaceExisting) {
        PjbAuthorizationTrailTemporalGranularity effectiveGranularity = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY : granularity;
        int effectiveLimit = Math.min(50000, Math.max(100, limit == null ? 10000 : limit));
        Instant effectiveAfter = occurredAfter == null ? Instant.now().minus(30, ChronoUnit.DAYS) : occurredAfter;
        Instant effectiveBefore = occurredBefore == null ? Instant.now() : occurredBefore;
        if (!effectiveBefore.isAfter(effectiveAfter)) {
            effectiveBefore = effectiveAfter.plus(1, effectiveGranularity == PjbAuthorizationTrailTemporalGranularity.HOUR ? ChronoUnit.HOURS : ChronoUnit.DAYS);
        }
        PjbAuthorizationTrailQueryCriteria criteria = new PjbAuthorizationTrailQueryCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                effectiveAfter,
                effectiveBefore,
                effectiveLimit
        );
        List<PjbAuthorizationTrailSnapshot> snapshots = readModelService.searchForForensics(criteria, effectiveLimit);
        Instant materializedAt = Instant.now();
        List<PjbAuthorizationTrailAnalyticsEntry> entries = materializationAssembler.assemble(effectiveGranularity, snapshots, materializedAt);
        Instant deleteStart = materializationAssembler.bucketStart(effectiveAfter, effectiveGranularity);
        Instant deleteEnd = materializationAssembler.bucketEnd(materializationAssembler.bucketStart(effectiveBefore, effectiveGranularity), effectiveGranularity);
        if (Boolean.TRUE.equals(replaceExisting) && deleteEnd.isAfter(deleteStart)) {
            analyticsRepository.deleteByGranularityAndBucketStartedAtGreaterThanEqualAndBucketStartedAtLessThan(
                    effectiveGranularity.name(),
                    toLocalDateTime(deleteStart),
                    toLocalDateTime(deleteEnd)
            );
        }
        int persistedBuckets = 0;
        if (!entries.isEmpty()) {
            List<PjbAuthorizationTrailAnalyticsEntry> persistable = Boolean.TRUE.equals(replaceExisting)
                    ? entries
                    : entries.stream()
                            .filter(entry -> !analyticsRepository.existsByAnalyticsKey(entry.getAnalyticsKey()))
                            .toList();
            persistedBuckets = persistable.size();
            if (!persistable.isEmpty()) {
                analyticsRepository.saveAll(persistable);
            }
        }
        return new PjbAuthorizationTrailAnalyticsMaterializationResponse(
                effectiveGranularity.name(),
                deleteStart,
                deleteEnd,
                snapshots.size(),
                persistedBuckets,
                materializedAt
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
