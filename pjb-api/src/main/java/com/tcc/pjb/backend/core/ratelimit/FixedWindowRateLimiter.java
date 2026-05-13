package com.tcc.pjb.backend.core.ratelimit;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class FixedWindowRateLimiter {

  private static final int MAX_BUCKETS = 100_000;
  private static final long CLEANUP_MASK = 255L;

  private final Clock clock;
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public FixedWindowRateLimiter(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  public boolean allow(String key, long windowMillis, long limit) {
    long now = clock.millis();
    long windowStart = now - (now % windowMillis);

    Bucket b = buckets.compute(key, (k, cur) -> {
      if (cur == null || cur.windowStart != windowStart) {
        return new Bucket(windowStart);
      }
      return cur;
    });

    long n = b.count.incrementAndGet();

    if ((now & CLEANUP_MASK) == 0 || buckets.size() > MAX_BUCKETS) {
      cleanup(windowStart);
    }

    return n <= limit;
  }

  private void cleanup(long activeWindowStart) {
    long minKeep = activeWindowStart - 120_000L;
    buckets.entrySet().removeIf(e -> e.getValue().windowStart < minKeep);
    int overflow = buckets.size() - MAX_BUCKETS;
    if (overflow <= 0) {
      return;
    }
    var iterator = buckets.entrySet().iterator();
    while (overflow > 0 && iterator.hasNext()) {
      iterator.next();
      iterator.remove();
      overflow--;
    }
  }

  private static final class Bucket {
    private final long windowStart;
    private final AtomicLong count = new AtomicLong();

    private Bucket(long windowStart) {
      this.windowStart = windowStart;
    }
  }
}
