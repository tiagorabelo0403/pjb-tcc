package com.tcc.pjb.backend.service.security.blocklist;

import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryBlocklistStore implements BlocklistStore, AutoCloseable {

  private static final int MAX_IPS = 200_000;
  private static final long CLEANUP_INITIAL_DELAY_SECONDS = 30L;
  private static final long CLEANUP_INTERVAL_SECONDS = 30L;

  private final ConcurrentHashMap<String, Entry> ips = new ConcurrentHashMap<>();
  private final ScheduledFuture<?> cleanupTask;

  public InMemoryBlocklistStore(ScheduledExecutorService cleanupScheduler) {
    this.cleanupTask = cleanupScheduler.scheduleWithFixedDelay(
        () -> PjbVirtualThreadSpine.start("blocklist-cleanup-task", this::cleanupExpiredSafely),
        CLEANUP_INITIAL_DELAY_SECONDS,
        CLEANUP_INTERVAL_SECONDS,
        TimeUnit.SECONDS
    );
  }

  @Override
  public void banIp(String ip, String reason, Duration ttl) {
    if (ip == null || ip.isBlank()) return;
    Duration effective = (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofHours(24) : ttl;
    Instant now = Instant.now();
    cleanupExpired(now);
    ips.put(ip, new Entry(now.plus(effective), reason));
    trimOverflow();
    log.warn("[BLOCKLIST:MEM] ip={} ttl={} reason={}", ip, effective, reason);
  }

  @Override
  public Optional<String> getReason(String ip) {
    if (ip == null || ip.isBlank()) return Optional.empty();
    Entry e = ips.get(ip);
    if (e == null) return Optional.empty();
    if (Instant.now().isAfter(e.until)) {
      ips.remove(ip);
      return Optional.empty();
    }
    return Optional.ofNullable(e.reason);
  }

  @Override
  public void unbanIp(String ip) {
    if (ip == null || ip.isBlank()) return;
    ips.remove(ip);
  }

  private void cleanupExpiredSafely() {
    try {
      cleanupExpired();
    } catch (Exception e) {
      log.debug("[BLOCKLIST:MEM] cleanup_failed", e);
    }
  }

  private void cleanupExpired() {
    cleanupExpired(Instant.now());
  }

  private void cleanupExpired(Instant now) {
    for (Map.Entry<String, Entry> e : ips.entrySet()) {
      if (e == null) {
        continue;
      }
      Entry v = e.getValue();
      if (v == null) {
        continue;
      }
      if (now.isAfter(v.until)) {
        ips.remove(e.getKey(), v);
      }
    }
  }

  private void trimOverflow() {
    int overflow = ips.size() - MAX_IPS;
    if (overflow <= 0) {
      return;
    }
    ips.entrySet().stream()
        .sorted(Map.Entry.comparingByValue(java.util.Comparator.comparing(Entry::until)))
        .limit(overflow)
        .map(Map.Entry::getKey)
        .toList()
        .forEach(ips::remove);
  }

  @Override
  public void close() {
    if (cleanupTask != null) {
      cleanupTask.cancel(false);
    }
  }

  private record Entry(Instant until, String reason) implements Comparable<Entry> {
    @Override
    public int compareTo(Entry other) {
      if (other == null) {
        return 1;
      }
      return until.compareTo(other.until);
    }
  }
}
