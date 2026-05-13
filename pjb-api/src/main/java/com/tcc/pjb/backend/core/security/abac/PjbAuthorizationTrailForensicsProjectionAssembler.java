package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailBucketResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailForensicsResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailForensicsSummaryResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailTimeBucketResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class PjbAuthorizationTrailForensicsProjectionAssembler {

    PjbAuthorizationTrailForensicsResponse assemble(PjbAuthorizationTrailTemporalGranularity granularity,
                                                    int limitApplied,
                                                    List<PjbAuthorizationTrailSnapshot> snapshots) {
        List<PjbAuthorizationTrailSnapshot> safeSnapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
        return new PjbAuthorizationTrailForensicsResponse(
                "PERSISTED",
                (granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY : granularity).name(),
                safeSnapshots.size(),
                Math.max(1, limitApplied),
                summary(safeSnapshots),
                timeSeries(granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY : granularity, safeSnapshots),
                bucket(safeSnapshots, PjbAuthorizationTrailSnapshot::integrationCode),
                bucket(safeSnapshots, PjbAuthorizationTrailSnapshot::institutionalUnitCode),
                bucket(safeSnapshots, PjbAuthorizationTrailSnapshot::resourceType),
                bucket(safeSnapshots, PjbAuthorizationTrailSnapshot::governanceScope)
        );
    }

    private PjbAuthorizationTrailForensicsSummaryResponse summary(List<PjbAuthorizationTrailSnapshot> snapshots) {
        long total = snapshots.size();
        long allowed = snapshots.stream().filter(PjbAuthorizationTrailSnapshot::allowed).count();
        long denied = total - allowed;
        long critico = snapshots.stream().filter(snapshot -> snapshot.riskLevel() == PjbAuthorizationRiskLevel.CRITICO).count();
        long governanceDenied = snapshots.stream().filter(snapshot -> snapshot.governanceRequired() && !snapshot.governanceSatisfied()).count();
        long stepUpDenied = snapshots.stream().filter(snapshot -> snapshot.stepUpRequired() && !snapshot.stepUpSatisfied()).count();
        Instant oldest = snapshots.stream().map(PjbAuthorizationTrailSnapshot::occurredAt).min(Comparator.naturalOrder()).orElse(null);
        Instant newest = snapshots.stream().map(PjbAuthorizationTrailSnapshot::occurredAt).max(Comparator.naturalOrder()).orElse(null);
        return new PjbAuthorizationTrailForensicsSummaryResponse(total, allowed, denied, critico, governanceDenied, stepUpDenied, oldest, newest);
    }

    private List<PjbAuthorizationTrailTimeBucketResponse> timeSeries(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                     List<PjbAuthorizationTrailSnapshot> snapshots) {
        Map<Instant, MutableTimeBucket> buckets = new LinkedHashMap<>();
        for (PjbAuthorizationTrailSnapshot snapshot : snapshots) {
            Instant start = granularity.bucketStart(snapshot.occurredAt());
            buckets.computeIfAbsent(start, ignored -> new MutableTimeBucket(granularity.bucketEndExclusive(snapshot.occurredAt())))
                    .include(snapshot);
        }
        return buckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();
    }

    private List<PjbAuthorizationTrailBucketResponse> bucket(List<PjbAuthorizationTrailSnapshot> snapshots,
                                                             Function<PjbAuthorizationTrailSnapshot, String> classifier) {
        Map<String, MutableBucket> map = new LinkedHashMap<>();
        for (PjbAuthorizationTrailSnapshot snapshot : snapshots) {
            String code = classifier.apply(snapshot);
            if (code == null || code.isBlank() || "NONE".equals(code)) {
                continue;
            }
            map.computeIfAbsent(code, ignored -> new MutableBucket()).include(snapshot.allowed());
        }
        return map.entrySet().stream()
                .map(entry -> new PjbAuthorizationTrailBucketResponse(entry.getKey(), entry.getValue().total, entry.getValue().allowed, entry.getValue().denied))
                .sorted(Comparator.comparingLong(PjbAuthorizationTrailBucketResponse::total).reversed()
                        .thenComparing(PjbAuthorizationTrailBucketResponse::code))
                .limit(20)
                .toList();
    }

    private static final class MutableBucket {
        private long total;
        private long allowed;
        private long denied;

        void include(boolean granted) {
            total++;
            if (granted) {
                allowed++;
            } else {
                denied++;
            }
        }
    }

    private static final class MutableTimeBucket {
        private final Instant endExclusive;
        private long total;
        private long allowed;
        private long denied;
        private long critico;
        private long governanceDenied;
        private long stepUpDenied;

        private MutableTimeBucket(Instant endExclusive) {
            this.endExclusive = endExclusive;
        }

        void include(PjbAuthorizationTrailSnapshot snapshot) {
            total++;
            if (snapshot.allowed()) {
                allowed++;
            } else {
                denied++;
            }
            if (snapshot.riskLevel() == PjbAuthorizationRiskLevel.CRITICO) {
                critico++;
            }
            if (snapshot.governanceRequired() && !snapshot.governanceSatisfied()) {
                governanceDenied++;
            }
            if (snapshot.stepUpRequired() && !snapshot.stepUpSatisfied()) {
                stepUpDenied++;
            }
        }

        PjbAuthorizationTrailTimeBucketResponse toResponse(Instant start) {
            return new PjbAuthorizationTrailTimeBucketResponse(
                    start,
                    endExclusive,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(start.atOffset(ZoneOffset.UTC)),
                    total,
                    allowed,
                    denied,
                    critico,
                    governanceDenied,
                    stepUpDenied
            );
        }
    }
}
