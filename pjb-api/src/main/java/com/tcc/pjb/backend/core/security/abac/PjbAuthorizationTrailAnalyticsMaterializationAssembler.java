package com.tcc.pjb.backend.core.security.abac;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class PjbAuthorizationTrailAnalyticsMaterializationAssembler {

    public List<PjbAuthorizationTrailAnalyticsEntry> assemble(PjbAuthorizationTrailTemporalGranularity granularity,
                                                              List<PjbAuthorizationTrailSnapshot> snapshots,
                                                              Instant materializedAt) {
        PjbAuthorizationTrailTemporalGranularity effectiveGranularity = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY : granularity;
        List<PjbAuthorizationTrailSnapshot> safeSnapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
        LinkedHashMap<AnalyticsKey, MutableAnalytics> aggregates = new LinkedHashMap<>();
        for (PjbAuthorizationTrailSnapshot snapshot : safeSnapshots) {
            if (snapshot == null || snapshot.occurredAt() == null) {
                continue;
            }
            Instant bucketStartedAt = bucketStart(snapshot.occurredAt(), effectiveGranularity);
            Instant bucketEndedAtExclusive = bucketEnd(bucketStartedAt, effectiveGranularity);
            accumulate(aggregates, new AnalyticsKey(bucketStartedAt, bucketEndedAtExclusive, PjbAuthorizationTrailAnalyticsDimensionType.OVERVIEW, "ALL", "ALL"), snapshot);
            accumulate(aggregates, new AnalyticsKey(bucketStartedAt, bucketEndedAtExclusive, PjbAuthorizationTrailAnalyticsDimensionType.ACTION, normalize(snapshot.action(), "NONE"), normalize(snapshot.action(), "NONE")), snapshot);
            accumulate(aggregates, new AnalyticsKey(bucketStartedAt, bucketEndedAtExclusive, PjbAuthorizationTrailAnalyticsDimensionType.RESOURCE_TYPE, normalize(snapshot.resourceType(), "NONE"), normalize(snapshot.resourceType(), "NONE")), snapshot);
            accumulate(aggregates, new AnalyticsKey(bucketStartedAt, bucketEndedAtExclusive, PjbAuthorizationTrailAnalyticsDimensionType.INTEGRATION, normalizeDimension(snapshot.integrationCode(), "SEM_INTEGRACAO"), normalizeDimension(snapshot.integrationCode(), "SEM_INTEGRACAO")), snapshot);
            accumulate(aggregates, new AnalyticsKey(bucketStartedAt, bucketEndedAtExclusive, PjbAuthorizationTrailAnalyticsDimensionType.INSTITUTIONAL_UNIT, normalizeDimension(snapshot.institutionalUnitCode(), "SEM_UNIDADE"), normalizeDimension(snapshot.institutionalUnitCode(), "SEM_UNIDADE")), snapshot);
            accumulate(aggregates, new AnalyticsKey(bucketStartedAt, bucketEndedAtExclusive, PjbAuthorizationTrailAnalyticsDimensionType.GOVERNANCE_SCOPE, normalizeDimension(snapshot.governanceScope(), "SEM_GOVERNANCA"), normalizeDimension(snapshot.governanceScope(), "SEM_GOVERNANCA")), snapshot);
            accumulate(aggregates, new AnalyticsKey(bucketStartedAt, bucketEndedAtExclusive, PjbAuthorizationTrailAnalyticsDimensionType.CAPABILITY, normalizeDimension(snapshot.institutionalCapabilityCode(), "SEM_CAPACIDADE"), normalizeDimension(snapshot.institutionalCapabilityCode(), "SEM_CAPACIDADE")), snapshot);
        }
        ArrayList<PjbAuthorizationTrailAnalyticsEntry> entries = new ArrayList<>(aggregates.size());
        aggregates.values().stream()
                .sorted(Comparator.comparing(MutableAnalytics::bucketStartedAt)
                        .thenComparing(MutableAnalytics::dimensionType)
                        .thenComparing(MutableAnalytics::dimensionCode))
                .map(aggregate -> aggregate.toEntry(effectiveGranularity, materializedAt == null ? Instant.now() : materializedAt, safeSnapshots.size()))
                .forEach(entries::add);
        return entries;
    }

    public Instant bucketStart(Instant instant, PjbAuthorizationTrailTemporalGranularity granularity) {
        LocalDateTime value = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime bucket = granularity == PjbAuthorizationTrailTemporalGranularity.HOUR
                ? value.truncatedTo(ChronoUnit.HOURS)
                : value.toLocalDate().atStartOfDay();
        return bucket.toInstant(ZoneOffset.UTC);
    }

    public Instant bucketEnd(Instant bucketStartedAt, PjbAuthorizationTrailTemporalGranularity granularity) {
        return granularity == PjbAuthorizationTrailTemporalGranularity.HOUR
                ? bucketStartedAt.plus(1, ChronoUnit.HOURS)
                : bucketStartedAt.plus(1, ChronoUnit.DAYS);
    }

    private void accumulate(Map<AnalyticsKey, MutableAnalytics> aggregates,
                            AnalyticsKey key,
                            PjbAuthorizationTrailSnapshot snapshot) {
        aggregates.computeIfAbsent(key, ignored -> new MutableAnalytics(key)).add(snapshot);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String normalizeDimension(String value, String fallback) {
        String normalized = normalize(value, fallback);
        return "NONE".equalsIgnoreCase(normalized) ? fallback : normalized;
    }

    private record AnalyticsKey(Instant bucketStartedAt,
                                Instant bucketEndedAtExclusive,
                                PjbAuthorizationTrailAnalyticsDimensionType dimensionType,
                                String dimensionCode,
                                String dimensionLabel) {
    }

    private static final class MutableAnalytics {
        private final Instant bucketStartedAt;
        private final Instant bucketEndedAtExclusive;
        private final PjbAuthorizationTrailAnalyticsDimensionType dimensionType;
        private final String dimensionCode;
        private final String dimensionLabel;
        private long total;
        private long allowed;
        private long denied;
        private long critico;
        private long governanceDenied;
        private long stepUpDenied;
        private Instant firstOccurredAt;
        private Instant lastOccurredAt;
        private final LinkedHashSet<String> requestIds = new LinkedHashSet<>();
        private final LinkedHashSet<Long> actorIds = new LinkedHashSet<>();

        private MutableAnalytics(AnalyticsKey key) {
            this.bucketStartedAt = key.bucketStartedAt();
            this.bucketEndedAtExclusive = key.bucketEndedAtExclusive();
            this.dimensionType = key.dimensionType();
            this.dimensionCode = key.dimensionCode();
            this.dimensionLabel = key.dimensionLabel();
        }

        private void add(PjbAuthorizationTrailSnapshot snapshot) {
            total++;
            if (snapshot.allowed()) {
                allowed++;
            } else {
                denied++;
            }
            if (snapshot.riskLevel() == PjbAuthorizationRiskLevel.CRITICO) {
                critico++;
            }
            if (!snapshot.allowed() && snapshot.governanceRequired() && !snapshot.governanceSatisfied()) {
                governanceDenied++;
            }
            if (!snapshot.allowed() && snapshot.stepUpRequired() && !snapshot.stepUpSatisfied()) {
                stepUpDenied++;
            }
            if (snapshot.requestId() != null && !snapshot.requestId().isBlank()) {
                requestIds.add(snapshot.requestId());
            }
            if (snapshot.actorId() != null) {
                actorIds.add(snapshot.actorId());
            }
            firstOccurredAt = firstOccurredAt == null || snapshot.occurredAt().isBefore(firstOccurredAt) ? snapshot.occurredAt() : firstOccurredAt;
            lastOccurredAt = lastOccurredAt == null || snapshot.occurredAt().isAfter(lastOccurredAt) ? snapshot.occurredAt() : lastOccurredAt;
        }

        private Instant bucketStartedAt() {
            return bucketStartedAt;
        }

        private String dimensionType() {
            return dimensionType.name();
        }

        private String dimensionCode() {
            return dimensionCode;
        }

        private PjbAuthorizationTrailAnalyticsEntry toEntry(PjbAuthorizationTrailTemporalGranularity granularity,
                                                            Instant materializedAt,
                                                            long sourceEventCount) {
            return PjbAuthorizationTrailAnalyticsEntry.of(
                    granularity,
                    bucketStartedAt,
                    bucketEndedAtExclusive,
                    dimensionType,
                    dimensionCode,
                    dimensionLabel,
                    total,
                    allowed,
                    denied,
                    critico,
                    governanceDenied,
                    stepUpDenied,
                    requestIds.size(),
                    actorIds.size(),
                    firstOccurredAt,
                    lastOccurredAt,
                    materializedAt,
                    sourceEventCount
            );
        }
    }
}
