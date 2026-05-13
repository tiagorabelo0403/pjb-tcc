package com.tcc.pjb.backend.platform.security.ratelimit;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class LocalSlidingWindowRateLimitStore implements CapabilityRateLimitStore {

    private static final int MAX_WINDOWS = 100_000;

    private final ConcurrentMap<String, SlidingWindow> windows = new ConcurrentHashMap<>();

    @Override
    public CapabilityRateLimitDecision tryConsume(String key,
                                                  long nowEpochSecond,
                                                  int windowSeconds,
                                                  int limitTokens,
                                                  int costTokens) {
        Objects.requireNonNull(key, "key");
        if (windowSeconds <= 0) throw new IllegalArgumentException("windowSeconds must be > 0");
        if (limitTokens <= 0) return new CapabilityRateLimitDecision(true, 0, 0, 0, windowSeconds, costTokens);
        if (costTokens <= 0) costTokens = 1;

        cleanup(nowEpochSecond, windowSeconds, false);
        SlidingWindow w = windows.computeIfAbsent(key, k -> new SlidingWindow(windowSeconds));
        CapabilityRateLimitDecision decision = w.tryConsume(nowEpochSecond, windowSeconds, limitTokens, costTokens);
        if (windows.size() > MAX_WINDOWS) {
            cleanup(nowEpochSecond, windowSeconds, true);
        }
        return decision;
    }

    private void cleanup(long nowEpochSecond, int windowSeconds, boolean force) {
        if (!force && windows.size() < Math.max(128, MAX_WINDOWS / 2)) {
            return;
        }
        long staleBefore = nowEpochSecond - Math.max(2L, windowSeconds * 2L);
        for (Iterator<Map.Entry<String, SlidingWindow>> iterator = windows.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, SlidingWindow> entry = iterator.next();
            SlidingWindow window = entry.getValue();
            if (window == null || window.lastSeenEpochSecond() < staleBefore) {
                iterator.remove();
            }
        }
        int overflow = windows.size() - MAX_WINDOWS;
        if (overflow <= 0) {
            return;
        }
        Iterator<Map.Entry<String, SlidingWindow>> iterator = windows.entrySet().iterator();
        while (overflow > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            overflow--;
        }
    }

    private static final class SlidingWindow {

        private final Bucket[] buckets;
        private final AtomicLong lastSeenEpochSecond = new AtomicLong(0);

        private SlidingWindow(int size) {
            this.buckets = new Bucket[size];
            for (int i = 0; i < size; i++) {
                buckets[i] = new Bucket();
            }
        }

        private CapabilityRateLimitDecision tryConsume(long nowEpochSecond,
                                                       int windowSeconds,
                                                       int limitTokens,
                                                       int costTokens) {
            lastSeenEpochSecond.accumulateAndGet(nowEpochSecond, Math::max);
            long total = 0;
            long oldestEligible = nowEpochSecond - windowSeconds + 1;
            for (Bucket b : buckets) {
                long ts = b.epochSecond.get();
                if (ts >= oldestEligible) {
                    long c = b.count.get();
                    if (c > 0) total += c;
                }
            }

            if (total + costTokens > limitTokens) {
                long retry = estimateRetryAfter(nowEpochSecond, windowSeconds, total + costTokens - limitTokens);
                return new CapabilityRateLimitDecision(false, limitTokens, 0, retry, windowSeconds, costTokens);
            }

            int idx = (int) Math.floorMod(nowEpochSecond, buckets.length);
            Bucket current = buckets[idx];
            current.rotateIfNeeded(nowEpochSecond);
            current.count.getAndAdd(costTokens);

            long remaining = Math.max(0L, limitTokens - (total + costTokens));
            return new CapabilityRateLimitDecision(true, limitTokens, remaining, 0, windowSeconds, costTokens);
        }

        private long estimateRetryAfter(long nowEpochSecond, int windowSeconds, long over) {
            long oldestEligible = nowEpochSecond - windowSeconds + 1;
            long[] ts = new long[buckets.length];
            long[] ct = new long[buckets.length];
            int n = 0;
            for (Bucket b : buckets) {
                long t = b.epochSecond.get();
                if (t >= oldestEligible) {
                    long c = b.count.get();
                    if (c > 0) {
                        ts[n] = t;
                        ct[n] = c;
                        n++;
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (ts[j] < ts[i]) {
                        long t = ts[i]; ts[i] = ts[j]; ts[j] = t;
                        long c = ct[i]; ct[i] = ct[j]; ct[j] = c;
                    }
                }
            }

            long acc = 0;
            long retry = 1;
            for (int i = 0; i < n; i++) {
                acc += ct[i];
                if (acc >= over) {
                    retry = (ts[i] + windowSeconds) - nowEpochSecond;
                    break;
                }
            }
            if (retry < 1) retry = 1;
            return retry;
        }

        private long lastSeenEpochSecond() {
            return lastSeenEpochSecond.get();
        }
    }

    private static final class Bucket {
        private final AtomicLong epochSecond = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);

        private void rotateIfNeeded(long nowEpochSecond) {
            long prev = epochSecond.get();
            if (prev == nowEpochSecond) return;
            if (epochSecond.compareAndSet(prev, nowEpochSecond)) {
                count.set(0);
            }
        }
    }
}
