package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailBucketResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailEntryResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailQueryResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailSummaryResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class PjbAuthorizationTrailProjectionAssembler {

    PjbAuthorizationTrailQueryResponse assemble(PjbAuthorizationTrailSourceMode sourceMode,
                                                int totalEntriesPersisted,
                                                int totalEntriesRuntime,
                                                List<PjbAuthorizationTrailSnapshot> snapshots) {
        List<PjbAuthorizationTrailSnapshot> safeSnapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
        int totalAvailable = switch (sourceMode == null ? PjbAuthorizationTrailSourceMode.PERSISTED : sourceMode) {
            case RUNTIME -> totalEntriesRuntime;
            case PERSISTED -> totalEntriesPersisted;
            case MERGED -> Math.max(totalEntriesPersisted, totalEntriesRuntime);
        };
        return new PjbAuthorizationTrailQueryResponse(
                (sourceMode == null ? PjbAuthorizationTrailSourceMode.PERSISTED : sourceMode).name(),
                totalAvailable,
                totalEntriesPersisted,
                totalEntriesRuntime,
                safeSnapshots.size(),
                summary(safeSnapshots),
                safeSnapshots.stream().map(this::toEntry).toList()
        );
    }

    List<PjbAuthorizationTrailSnapshot> merge(List<PjbAuthorizationTrailSnapshot> persisted,
                                              List<PjbAuthorizationTrailSnapshot> runtime,
                                              int limit) {
        Map<String, PjbAuthorizationTrailSnapshot> unique = new LinkedHashMap<>();
        ArrayList<PjbAuthorizationTrailSnapshot> combined = new ArrayList<>();
        if (persisted != null) {
            combined.addAll(persisted);
        }
        if (runtime != null) {
            combined.addAll(runtime);
        }
        combined.stream()
                .sorted(Comparator.comparing(PjbAuthorizationTrailSnapshot::occurredAt).reversed()
                        .thenComparing(PjbAuthorizationTrailSnapshot::auditEventCode)
                        .thenComparing(PjbAuthorizationTrailSnapshot::payloadHash))
                .forEach(snapshot -> unique.putIfAbsent(identity(snapshot), snapshot));
        return unique.values().stream()
                .limit(Math.max(1, limit))
                .toList();
    }

    private String identity(PjbAuthorizationTrailSnapshot snapshot) {
        return snapshot.auditEventCode() + ':' + snapshot.payloadHash();
    }

    private PjbAuthorizationTrailEntryResponse toEntry(PjbAuthorizationTrailSnapshot snapshot) {
        return new PjbAuthorizationTrailEntryResponse(
                snapshot.occurredAt(),
                snapshot.auditEventCode(),
                snapshot.action(),
                snapshot.resourceType(),
                snapshot.resourceId(),
                snapshot.allowed(),
                snapshot.reason(),
                snapshot.policyVersion(),
                snapshot.actorType(),
                snapshot.actorId(),
                snapshot.requestId(),
                snapshot.justificativa(),
                snapshot.effectiveSigilo(),
                snapshot.riskLevel().name(),
                snapshot.riskScore(),
                snapshot.stepUpRequired(),
                snapshot.stepUpSatisfied(),
                snapshot.stepUpChannel(),
                snapshot.stepUpCode(),
                snapshot.governanceRequired(),
                snapshot.governanceSatisfied(),
                snapshot.governanceChannel(),
                snapshot.governanceCode(),
                snapshot.governanceScope(),
                snapshot.integrationCode(),
                snapshot.institutionalUnitCode(),
                snapshot.institutionalBoxCode(),
                snapshot.institutionalCapabilityCode(),
                snapshot.expedicaoUuid(),
                snapshot.payloadHash(),
                snapshot.auditDescription()
        );
    }

    private PjbAuthorizationTrailSummaryResponse summary(List<PjbAuthorizationTrailSnapshot> snapshots) {
        long total = snapshots.size();
        long allowed = snapshots.stream().filter(PjbAuthorizationTrailSnapshot::allowed).count();
        long denied = total - allowed;
        long critico = snapshots.stream().filter(snapshot -> snapshot.riskLevel() == PjbAuthorizationRiskLevel.CRITICO).count();
        long governanceRequired = snapshots.stream().filter(PjbAuthorizationTrailSnapshot::governanceRequired).count();
        long governanceDenied = snapshots.stream().filter(snapshot -> snapshot.governanceRequired() && !snapshot.governanceSatisfied()).count();
        long stepUpRequired = snapshots.stream().filter(PjbAuthorizationTrailSnapshot::stepUpRequired).count();
        long stepUpDenied = snapshots.stream().filter(snapshot -> snapshot.stepUpRequired() && !snapshot.stepUpSatisfied()).count();
        return new PjbAuthorizationTrailSummaryResponse(
                total,
                allowed,
                denied,
                critico,
                governanceRequired,
                governanceDenied,
                stepUpRequired,
                stepUpDenied,
                bucket(snapshots, PjbAuthorizationTrailSnapshot::action),
                bucket(snapshots, PjbAuthorizationTrailSnapshot::resourceType),
                bucket(snapshots, PjbAuthorizationTrailSnapshot::integrationCode),
                bucket(snapshots, PjbAuthorizationTrailSnapshot::institutionalCapabilityCode)
        );
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
                .limit(12)
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
                return;
            }
            denied++;
        }
    }
}
