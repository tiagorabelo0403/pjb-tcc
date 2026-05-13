package com.tcc.pjb.backend.core.security.abac;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PjbAuthorizationTrailReadModelService {

    private final PjbAuthorizationTrailReadModelRepository repository;

    public PjbAuthorizationTrailReadModelService(PjbAuthorizationTrailReadModelRepository repository) {
        this.repository = repository;
    }

    public void materialize(PjbAuthorizationDecisionTrail trail) {
        materializeReturningSnapshot(trail);
    }

    public PjbAuthorizationTrailSnapshot materializeReturningSnapshot(PjbAuthorizationDecisionTrail trail) {
        PjbAuthorizationTrailSnapshot snapshot = PjbAuthorizationTrailSnapshot.from(trail);
        if (snapshot == null || repository.existsByPayloadHash(snapshot.payloadHash())) {
            return null;
        }
        try {
            repository.save(PjbAuthorizationTrailReadModelEntry.from(snapshot));
            return snapshot;
        } catch (DataIntegrityViolationException ignored) {
            return null;
        }
    }

    public List<PjbAuthorizationTrailSnapshot> search(PjbAuthorizationTrailQueryCriteria criteria) {
        PjbAuthorizationTrailQueryCriteria effectiveCriteria = criteria == null
                ? PjbAuthorizationTrailQueryCriteria.defaults()
                : criteria;
        return repository.search(
                        effectiveCriteria.actionPrefix(),
                        effectiveCriteria.resourceType(),
                        effectiveCriteria.resourceId(),
                        effectiveCriteria.actorId(),
                        effectiveCriteria.actorType(),
                        effectiveCriteria.allowed(),
                        effectiveCriteria.riskLevel() == null ? null : effectiveCriteria.riskLevel().name(),
                        effectiveCriteria.requestId(),
                        effectiveCriteria.integrationCode(),
                        effectiveCriteria.institutionalUnitCode(),
                        effectiveCriteria.institutionalBoxCode(),
                        effectiveCriteria.institutionalCapabilityCode(),
                        effectiveCriteria.governanceChannel(),
                        effectiveCriteria.governanceScope(),
                        effectiveCriteria.governanceRequired(),
                        effectiveCriteria.governanceSatisfied(),
                        effectiveCriteria.stepUpRequired(),
                        effectiveCriteria.stepUpSatisfied(),
                        toLocalDateTime(effectiveCriteria.occurredAfter()),
                        toLocalDateTime(effectiveCriteria.occurredBefore()),
                        PageRequest.of(0, effectiveCriteria.limit())
                )
                .stream()
                .map(PjbAuthorizationTrailReadModelEntry::toSnapshot)
                .toList();
    }

    public int totalEntries() {
        long count = repository.count();
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    public Instant oldestOccurredAt() {
        PjbAuthorizationTrailReadModelEntry entry = repository.findFirstByOrderByOccurredAtAscIdAsc();
        return entry == null ? null : entry.toSnapshot().occurredAt();
    }

    public Instant newestOccurredAt() {
        PjbAuthorizationTrailReadModelEntry entry = repository.findFirstByOrderByOccurredAtDescIdDesc();
        return entry == null ? null : entry.toSnapshot().occurredAt();
    }

    public int totalEntriesBefore(Instant cutoff) {
        if (cutoff == null) {
            return 0;
        }
        long count = repository.countByOccurredAtBefore(toLocalDateTime(cutoff));
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    public List<PjbAuthorizationTrailSnapshot> searchForForensics(PjbAuthorizationTrailQueryCriteria criteria, int limit) {
        PjbAuthorizationTrailQueryCriteria effectiveCriteria = criteria == null
                ? PjbAuthorizationTrailQueryCriteria.defaults()
                : criteria;
        int effectiveLimit = Math.min(10000, Math.max(1, limit <= 0 ? effectiveCriteria.limit() : limit));
        return repository.search(
                        effectiveCriteria.actionPrefix(),
                        effectiveCriteria.resourceType(),
                        effectiveCriteria.resourceId(),
                        effectiveCriteria.actorId(),
                        effectiveCriteria.actorType(),
                        effectiveCriteria.allowed(),
                        effectiveCriteria.riskLevel() == null ? null : effectiveCriteria.riskLevel().name(),
                        effectiveCriteria.requestId(),
                        effectiveCriteria.integrationCode(),
                        effectiveCriteria.institutionalUnitCode(),
                        effectiveCriteria.institutionalBoxCode(),
                        effectiveCriteria.institutionalCapabilityCode(),
                        effectiveCriteria.governanceChannel(),
                        effectiveCriteria.governanceScope(),
                        effectiveCriteria.governanceRequired(),
                        effectiveCriteria.governanceSatisfied(),
                        effectiveCriteria.stepUpRequired(),
                        effectiveCriteria.stepUpSatisfied(),
                        toLocalDateTime(effectiveCriteria.occurredAfter()),
                        toLocalDateTime(effectiveCriteria.occurredBefore()),
                        PageRequest.of(0, effectiveLimit)
                )
                .stream()
                .map(PjbAuthorizationTrailReadModelEntry::toSnapshot)
                .toList();
    }

    public List<PjbAuthorizationTrailSnapshot> searchWindow(Instant occurredAfterInclusive, Instant occurredBeforeExclusive) {
        if (occurredAfterInclusive == null || occurredBeforeExclusive == null || !occurredBeforeExclusive.isAfter(occurredAfterInclusive)) {
            return List.of();
        }
        return repository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscIdAsc(
                        toLocalDateTime(occurredAfterInclusive),
                        toLocalDateTime(occurredBeforeExclusive)
                )
                .stream()
                .map(PjbAuthorizationTrailReadModelEntry::toSnapshot)
                .toList();
    }

    private static LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
