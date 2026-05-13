package com.tcc.pjb.backend.service.secretariat.query.queue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;

@Service
public class SecretariatQueueLoadResolver {

  private final SecretariatQueueItemRepository repo;

  public SecretariatQueueLoadResolver(SecretariatQueueItemRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  public SecretariatQueueLoadProfile resolve(String inboxKey, List<String> statuses) {
    Objects.requireNonNull(inboxKey, "inboxKey");
    List<String> effectiveStatuses = statuses == null || statuses.isEmpty() ? List.of("PENDENTE", "EM_EXECUCAO") : List.copyOf(statuses);
    Instant now = Instant.now();
    Object[] raw = repo.loadSignature(inboxKey, effectiveStatuses, now, now.plus(24, ChronoUnit.HOURS));
    long total = value(raw, 0);
    long overdue = value(raw, 1);
    long critical = value(raw, 2);
    long due24h = value(raw, 3);
    String loadBand = resolveLoadBand(total, overdue, critical, due24h);
    String responseMode = resolveResponseMode(loadBand, overdue, critical, due24h);
    boolean rebalanceSuggested = "SATURATED".equals(loadBand) || ("HIGH".equals(loadBand) && (critical >= 8 || overdue >= 6));

    LinkedHashSet<String> markers = new LinkedHashSet<>();
    if (total == 0) {
      markers.add("EMPTY");
    }
    if (overdue > 0) {
      markers.add("OVERDUE");
    }
    if (critical > 0) {
      markers.add("CRITICAL");
    }
    if (due24h > 0) {
      markers.add("DUE_24H");
    }
    if (rebalanceSuggested) {
      markers.add("REBALANCE");
    }

    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("pressureScore", pressureScore(total, overdue, critical, due24h));
    metadata.put("statuses", effectiveStatuses);
    metadata.put("pressureRatio", total <= 0 ? 0.0d : round2((double) (overdue + critical + due24h) / (double) total));
    metadata.put("criticalRatio", total <= 0 ? 0.0d : round2((double) critical / (double) total));

    return new SecretariatQueueLoadProfile(
        inboxKey,
        total,
        overdue,
        critical,
        due24h,
        loadBand,
        responseMode,
        rebalanceSuggested,
        List.copyOf(markers),
        metadata
    );
  }

  private static String resolveLoadBand(long total, long overdue, long critical, long due24h) {
    if (total >= 240 || overdue >= 18 || critical >= 16 || pressureScore(total, overdue, critical, due24h) >= 80) {
      return "SATURATED";
    }
    if (total >= 100 || overdue >= 8 || critical >= 8 || due24h >= 24 || pressureScore(total, overdue, critical, due24h) >= 45) {
      return "HIGH";
    }
    if (total >= 40 || overdue >= 2 || critical >= 2 || due24h >= 8 || pressureScore(total, overdue, critical, due24h) >= 18) {
      return "MODERATE";
    }
    return "NORMAL";
  }

  private static String resolveResponseMode(String loadBand, long overdue, long critical, long due24h) {
    if ("SATURATED".equals(loadBand)) {
      return critical > 0 ? "REBALANCE_IMMEDIATE" : "CAPACITY_BREAKER";
    }
    if ("HIGH".equals(loadBand)) {
      return overdue > 0 ? "DEADLINE_RECOVERY" : "TRIAGE_EXPEDITE";
    }
    if (due24h > 0) {
      return "WINDOW_PROTECTION";
    }
    return "FLOW_STANDARD";
  }

  private static int pressureScore(long total, long overdue, long critical, long due24h) {
    long score = total / 8 + overdue * 3 + critical * 4 + due24h * 2;
    return (int) Math.min(score, 100L);
  }

  private static double round2(double value) {
    return Math.round(value * 100.0d) / 100.0d;
  }

  private static long value(Object[] raw, int index) {
    if (raw == null || index < 0 || index >= raw.length || raw[index] == null) {
      return 0L;
    }
    return ((Number) raw[index]).longValue();
  }
}
