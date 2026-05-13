package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsDimensionResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailBucketResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailTimeBucketResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PjbAuthorizationTrailAnalyticsProjectionAssembler {

    public PjbAuthorizationTrailAnalyticsResponse assemble(PjbAuthorizationTrailTemporalGranularity granularity,
                                                           int topN,
                                                           List<PjbAuthorizationTrailAnalyticsEntry> entries) {
        List<PjbAuthorizationTrailAnalyticsEntry> safeEntries = entries == null ? List.of() : List.copyOf(entries);
        ArrayList<PjbAuthorizationTrailTimeBucketResponse> timeSeries = new ArrayList<>();
        LinkedHashMap<PjbAuthorizationTrailAnalyticsDimensionType, Map<String, AggregateBucket>> groupedDimensions = new LinkedHashMap<>();
        Instant windowStartedAt = null;
        Instant windowEndedAtExclusive = null;
        Instant latestMaterializedAt = null;
        long persistedBucketCount = safeEntries.size();
        long representedEvents = 0L;
        for (PjbAuthorizationTrailAnalyticsEntry entry : safeEntries) {
            Instant bucketStartedAt = entry.bucketStartedAtInstant();
            Instant bucketEndedAtExclusive = entry.bucketEndedAtExclusiveInstant();
            windowStartedAt = windowStartedAt == null || bucketStartedAt.isBefore(windowStartedAt) ? bucketStartedAt : windowStartedAt;
            windowEndedAtExclusive = windowEndedAtExclusive == null || bucketEndedAtExclusive.isAfter(windowEndedAtExclusive) ? bucketEndedAtExclusive : windowEndedAtExclusive;
            latestMaterializedAt = latestMaterializedAt == null || entry.materializedAtInstant().isAfter(latestMaterializedAt) ? entry.materializedAtInstant() : latestMaterializedAt;
            PjbAuthorizationTrailAnalyticsDimensionType dimensionType = PjbAuthorizationTrailAnalyticsDimensionType.valueOf(entry.getDimensionType());
            if (dimensionType == PjbAuthorizationTrailAnalyticsDimensionType.OVERVIEW) {
                representedEvents += entry.getTotalCount();
                timeSeries.add(new PjbAuthorizationTrailTimeBucketResponse(
                        bucketStartedAt,
                        bucketEndedAtExclusive,
                        bucketLabel(bucketStartedAt, granularity),
                        entry.getTotalCount(),
                        entry.getAllowedCount(),
                        entry.getDeniedCount(),
                        entry.getCriticoCount(),
                        entry.getGovernanceDeniedCount(),
                        entry.getStepUpDeniedCount()
                ));
            } else {
                groupedDimensions
                        .computeIfAbsent(dimensionType, ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(entry.getDimensionCode(), ignored -> new AggregateBucket(entry.getDimensionCode()))
                        .add(entry);
            }
        }
        List<PjbAuthorizationTrailAnalyticsDimensionResponse> dimensions = groupedDimensions.entrySet().stream()
                .map(entry -> new PjbAuthorizationTrailAnalyticsDimensionResponse(
                        entry.getKey().name(),
                        entry.getValue().values().stream()
                                .sorted(Comparator.comparingLong(AggregateBucket::total).reversed().thenComparing(AggregateBucket::code))
                                .limit(Math.max(1, topN))
                                .map(AggregateBucket::toResponse)
                                .toList()
                ))
                .toList();
        timeSeries.sort(Comparator.comparing(PjbAuthorizationTrailTimeBucketResponse::startedAt));
        return new PjbAuthorizationTrailAnalyticsResponse(
                granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY.name() : granularity.name(),
                windowStartedAt,
                windowEndedAtExclusive,
                latestMaterializedAt,
                persistedBucketCount,
                representedEvents,
                timeSeries,
                dimensions
        );
    }

    private static String bucketLabel(Instant bucketStartedAt, PjbAuthorizationTrailTemporalGranularity granularity) {
        String value = bucketStartedAt == null ? Instant.EPOCH.toString() : bucketStartedAt.toString();
        return granularity == PjbAuthorizationTrailTemporalGranularity.HOUR
                ? value.substring(0, Math.min(13, value.length())) + ":00Z"
                : value.substring(0, Math.min(10, value.length()));
    }

    private static final class AggregateBucket {
        private final String code;
        private long total;
        private long allowed;
        private long denied;

        private AggregateBucket(String code) {
            this.code = code == null || code.isBlank() ? "NONE" : code;
        }

        private void add(PjbAuthorizationTrailAnalyticsEntry entry) {
            total += entry.getTotalCount();
            allowed += entry.getAllowedCount();
            denied += entry.getDeniedCount();
        }

        private String code() {
            return code;
        }

        private long total() {
            return total;
        }

        private PjbAuthorizationTrailBucketResponse toResponse() {
            return new PjbAuthorizationTrailBucketResponse(code, total, allowed, denied);
        }
    }
}
