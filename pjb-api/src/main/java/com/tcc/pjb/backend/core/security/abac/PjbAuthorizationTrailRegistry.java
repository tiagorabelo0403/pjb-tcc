package com.tcc.pjb.backend.core.security.abac;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class PjbAuthorizationTrailRegistry {

    private static final int MAX_ENTRIES = 5000;

    private final ConcurrentLinkedDeque<PjbAuthorizationTrailSnapshot> snapshots = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger();

    public void register(PjbAuthorizationDecisionTrail trail) {
        PjbAuthorizationTrailSnapshot snapshot = PjbAuthorizationTrailSnapshot.from(trail);
        if (snapshot == null) {
            return;
        }
        snapshots.addFirst(snapshot);
        size.incrementAndGet();
        trimOverflow();
    }

    public List<PjbAuthorizationTrailSnapshot> search(PjbAuthorizationTrailQueryCriteria criteria) {
        PjbAuthorizationTrailQueryCriteria effectiveCriteria = criteria == null
                ? PjbAuthorizationTrailQueryCriteria.defaults()
                : criteria;
        return snapshots.stream()
                .filter(effectiveCriteria::matches)
                .limit(effectiveCriteria.limit())
                .toList();
    }

    public int totalEntries() {
        return Math.max(0, size.get());
    }

    private void trimOverflow() {
        while (size.get() > MAX_ENTRIES) {
            PjbAuthorizationTrailSnapshot removed = snapshots.pollLast();
            if (removed == null) {
                size.set(snapshots.size());
                return;
            }
            size.decrementAndGet();
        }
    }
}
