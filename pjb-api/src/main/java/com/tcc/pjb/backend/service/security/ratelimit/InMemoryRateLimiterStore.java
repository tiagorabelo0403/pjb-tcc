package com.tcc.pjb.backend.service.security.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryRateLimiterStore implements RateLimiterStore {

    private static final int MAX_ENTRIES = 200_000;
    private static final long CLEANUP_INTERVAL_NANOS = Duration.ofSeconds(30).toNanos();

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();
    private final AtomicLong nextCleanupAtNanos = new AtomicLong(System.nanoTime() + CLEANUP_INTERVAL_NANOS);

    @Override
    public long incr(String key, Duration ttl) {
        if (key == null || key.isBlank()) return 0L;
        Duration effective = (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofMinutes(1) : ttl;
        Instant now = Instant.now();
        cleanupIfRequired(now, false);

        Entry e = map.compute(key, (k, current) -> {
            if (current == null || current.isExpired(now)) {
                return new Entry(new AtomicLong(1), now.plus(effective));
            }
            current.counter.incrementAndGet();
            return current;
        });

        if (map.size() > MAX_ENTRIES) {
            cleanupIfRequired(now, true);
        }
        return e.counter.get();
    }

    private void cleanupIfRequired(Instant now, boolean force) {
        long instant = System.nanoTime();
        long scheduled = nextCleanupAtNanos.get();
        if (!force && instant < scheduled) {
            return;
        }
        if (!nextCleanupAtNanos.compareAndSet(scheduled, instant + CLEANUP_INTERVAL_NANOS) && !force) {
            return;
        }
        pruneExpired(now);
        if (map.size() > MAX_ENTRIES) {
            trimOverflow();
        }
    }

    private void pruneExpired(Instant now) {
        for (Iterator<Map.Entry<String, Entry>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Entry> entry = iterator.next();
            Entry value = entry.getValue();
            if (value == null || value.isExpired(now)) {
                iterator.remove();
            }
        }
    }

    private void trimOverflow() {
        int overflow = map.size() - MAX_ENTRIES;
        if (overflow <= 0) {
            return;
        }
        Iterator<Map.Entry<String, Entry>> iterator = map.entrySet().iterator();
        while (overflow > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            overflow--;
        }
    }

    private static final class Entry {
        private final AtomicLong counter;
        private final Instant expiresAt;

        private Entry(AtomicLong counter, Instant expiresAt) {
            this.counter = counter;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired(Instant now) {
            return expiresAt == null || now.isAfter(expiresAt);
        }
    }
}
