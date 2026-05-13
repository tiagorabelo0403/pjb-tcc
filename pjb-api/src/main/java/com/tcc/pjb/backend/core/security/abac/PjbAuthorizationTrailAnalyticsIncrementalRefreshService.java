package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsMaterializationResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbAuthorizationTrailAnalyticsIncrementalRefreshService {

    private final PjbAuthorizationTrailReadModelService readModelService;
    private final PjbAuthorizationTrailAnalyticsRepository analyticsRepository;
    private final PjbAuthorizationTrailAnalyticsMaterializationAssembler materializationAssembler;

    public PjbAuthorizationTrailAnalyticsIncrementalRefreshService(PjbAuthorizationTrailReadModelService readModelService,
                                                                   PjbAuthorizationTrailAnalyticsRepository analyticsRepository) {
        this.readModelService = readModelService;
        this.analyticsRepository = analyticsRepository;
        this.materializationAssembler = new PjbAuthorizationTrailAnalyticsMaterializationAssembler();
    }

    @Transactional
    public PjbAuthorizationTrailAnalyticsMaterializationResponse refreshBucket(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                               Instant bucketReference) {
        PjbAuthorizationTrailTemporalGranularity effectiveGranularity = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY : granularity;
        Instant reference = bucketReference == null ? Instant.now() : bucketReference;
        Instant bucketStartedAt = effectiveGranularity.bucketStart(reference);
        Instant bucketEndedAtExclusive = effectiveGranularity.bucketEndExclusive(reference);
        List<PjbAuthorizationTrailSnapshot> snapshots = readModelService.searchWindow(bucketStartedAt, bucketEndedAtExclusive);
        Instant materializedAt = Instant.now();
        List<PjbAuthorizationTrailAnalyticsEntry> recalculatedEntries = materializationAssembler.assemble(effectiveGranularity, snapshots, materializedAt);
        int persistedBuckets = 0;
        for (PjbAuthorizationTrailAnalyticsEntry entry : recalculatedEntries) {
            upsert(entry);
            persistedBuckets++;
        }
        return new PjbAuthorizationTrailAnalyticsMaterializationResponse(
                effectiveGranularity.name(),
                bucketStartedAt,
                bucketEndedAtExclusive,
                snapshots.size(),
                persistedBuckets,
                materializedAt
        );
    }

    private void upsert(PjbAuthorizationTrailAnalyticsEntry incoming) {
        PjbAuthorizationTrailAnalyticsEntry current = analyticsRepository.findByAnalyticsKey(incoming.getAnalyticsKey()).orElse(null);
        if (current == null) {
            try {
                analyticsRepository.save(incoming);
                return;
            } catch (DataIntegrityViolationException ignored) {
                current = analyticsRepository.findByAnalyticsKey(incoming.getAnalyticsKey()).orElse(null);
                if (current == null) {
                    return;
                }
            }
        }
        copyMutableState(current, incoming);
        analyticsRepository.save(current);
    }

    private void copyMutableState(PjbAuthorizationTrailAnalyticsEntry target,
                                  PjbAuthorizationTrailAnalyticsEntry source) {
        target.setBucketEndedAtExclusive(toLocalDateTime(source.bucketEndedAtExclusiveInstant()));
        target.setDimensionLabel(source.getDimensionLabel());
        target.setTotalCount(source.getTotalCount());
        target.setAllowedCount(source.getAllowedCount());
        target.setDeniedCount(source.getDeniedCount());
        target.setCriticoCount(source.getCriticoCount());
        target.setGovernanceDeniedCount(source.getGovernanceDeniedCount());
        target.setStepUpDeniedCount(source.getStepUpDeniedCount());
        target.setUniqueRequestCount(source.getUniqueRequestCount());
        target.setUniqueActorCount(source.getUniqueActorCount());
        target.setFirstOccurredAt(toLocalDateTime(source.getFirstOccurredAt() == null ? Instant.EPOCH : source.getFirstOccurredAt().toInstant(ZoneOffset.UTC)));
        target.setLastOccurredAt(toLocalDateTime(source.getLastOccurredAt() == null ? Instant.EPOCH : source.getLastOccurredAt().toInstant(ZoneOffset.UTC)));
        target.setMaterializedAt(toLocalDateTime(source.materializedAtInstant()));
        target.setSourceEventCount(source.getSourceEventCount());
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant == null ? Instant.EPOCH : instant, ZoneOffset.UTC);
    }
}
