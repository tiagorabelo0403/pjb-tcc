package com.tcc.pjb.backend.service.secretariat.query.reference;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;

@Service
public class SecretariatDeskLoadResolver {

  private final SecretariatQueueItemRepository repo;

  public SecretariatDeskLoadResolver(SecretariatQueueItemRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  public SecretariatDeskLoadProfile resolve(String inboxKey, Collection<String> statuses, ForumDeskPortfolioProfile portfolio) {
    Objects.requireNonNull(inboxKey, "inboxKey");
    Objects.requireNonNull(portfolio, "portfolio");
    List<String> effectiveStatuses = statuses == null || statuses.isEmpty() ? List.of("PENDENTE", "EM_EXECUCAO") : List.copyOf(statuses);
    Instant now = Instant.now();
    List<Object[]> rows = repo.deskWorkload(inboxKey, effectiveStatuses, now);

    String dominantDesk = portfolio.triageDesk();
    long total = 0L;
    long overdue = 0L;
    long blocking = 0L;
    long secrecy = 0L;
    long hearing = 0L;
    int dominantScore = -1;
    LinkedHashMap<String, Object> deskSignals = new LinkedHashMap<>();

    for (Object[] row : rows) {
      String deskAxis = stringValue(row, 0, portfolio.triageDesk());
      long deskTotal = longValue(row, 1);
      long deskOverdue = longValue(row, 2);
      long deskBlocking = longValue(row, 3);
      long deskSecrecy = longValue(row, 4);
      long deskHearing = longValue(row, 5);
      int score = pressureScore(deskTotal, deskOverdue, deskBlocking, deskSecrecy, deskHearing);
      total += deskTotal;
      overdue += deskOverdue;
      blocking += deskBlocking;
      secrecy += deskSecrecy;
      hearing += deskHearing;

      LinkedHashMap<String, Object> signal = new LinkedHashMap<>();
      signal.put("total", deskTotal);
      signal.put("overdue", deskOverdue);
      signal.put("blocking", deskBlocking);
      signal.put("secrecy", deskSecrecy);
      signal.put("hearing", deskHearing);
      signal.put("pressureScore", score);
      deskSignals.put(deskAxis, Map.copyOf(signal));

      if (score > dominantScore) {
        dominantScore = score;
        dominantDesk = deskAxis;
      }
    }

    boolean secrecyPressure = secrecy > 0 && (secrecy >= 3 || secrecy * 3 >= Math.max(total, 1L));
    boolean hearingPressure = hearing > 0 && (hearing >= 3 || hearing * 3 >= Math.max(total, 1L));
    String loadBand = resolveLoadBand(total, overdue, blocking, secrecy, hearing);
    boolean forceRedistribution = "SATURATED".equals(loadBand)
        || blocking >= 5
        || overdue >= 6
        || secrecyPressure
        || hearingPressure;
    String redistributionDesk = resolveRedistributionDesk(forceRedistribution, secrecyPressure, hearingPressure, portfolio);
    String gabineteSupportDesk = portfolio.gabineteDesk();
    String coordinationMode = resolveCoordinationMode(loadBand, blocking, secrecyPressure, hearingPressure, portfolio);

    LinkedHashSet<String> labels = new LinkedHashSet<>();
    labels.add(normalizeAxis(dominantDesk));
    labels.add(loadBand);
    if (forceRedistribution) {
      labels.add("REDISTRIBUTION");
    }
    if (secrecyPressure) {
      labels.add("SECRECY_PRESSURE");
    }
    if (hearingPressure) {
      labels.add("HEARING_PRESSURE");
    }
    if (blocking > 0) {
      labels.add("BLOCKING");
    }

    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("deskSignals", deskSignals);
    metadata.put("pressureScore", pressureScore(total, overdue, blocking, secrecy, hearing));
    metadata.put("statuses", effectiveStatuses);
    metadata.put("portfolioDescriptor", portfolio.operationalDescriptor());
    metadata.put("coordinationDescriptor", portfolio.coordinationDescriptor());
    metadata.put("assistantDesk", portfolio.assistantDesk());
    metadata.put("coordinationDesk", portfolio.coordinationDesk());
    metadata.put("escalationDesk", portfolio.escalationDesk());

    return new SecretariatDeskLoadProfile(
        inboxKey,
        dominantDesk,
        total,
        overdue,
        blocking,
        secrecy,
        hearing,
        loadBand,
        redistributionDesk,
        gabineteSupportDesk,
        coordinationMode,
        forceRedistribution,
        secrecyPressure,
        hearingPressure,
        List.copyOf(labels),
        metadata
    );
  }

  private static String resolveLoadBand(long total, long overdue, long blocking, long secrecy, long hearing) {
    int score = pressureScore(total, overdue, blocking, secrecy, hearing);
    if (total >= 180 || overdue >= 12 || blocking >= 10 || score >= 75) {
      return "SATURATED";
    }
    if (total >= 60 || overdue >= 5 || blocking >= 4 || score >= 35) {
      return "HIGH";
    }
    if (total >= 18 || overdue >= 1 || blocking >= 1 || score >= 12) {
      return "MODERATE";
    }
    return "NORMAL";
  }

  private static String resolveRedistributionDesk(boolean forceRedistribution, boolean secrecyPressure, boolean hearingPressure, ForumDeskPortfolioProfile portfolio) {
    if (secrecyPressure) {
      return portfolio.escalationDesk();
    }
    if (hearingPressure) {
      return portfolio.hearingDesk();
    }
    if (forceRedistribution) {
      return portfolio.redistributionDesk();
    }
    return portfolio.assistantDesk();
  }

  private static String resolveCoordinationMode(String loadBand, long blocking, boolean secrecyPressure, boolean hearingPressure, ForumDeskPortfolioProfile portfolio) {
    if (secrecyPressure) {
      return "SIGILO_CONTROLLED:" + portfolio.coordinationDesk();
    }
    if (hearingPressure) {
      return "AUDIENCE_CONTROLLED:" + portfolio.coordinationDesk();
    }
    if ("SATURATED".equals(loadBand)) {
      return "CAPACITY_BREAKER:" + portfolio.redistributionDesk();
    }
    if (blocking > 0) {
      return "BLOCKING_RECOVERY:" + portfolio.complianceDesk();
    }
    if ("HIGH".equals(loadBand)) {
      return "DESK_REBALANCE:" + portfolio.assistantDesk();
    }
    return "FLOW_STANDARD:" + portfolio.coordinationDesk();
  }

  private static int pressureScore(long total, long overdue, long blocking, long secrecy, long hearing) {
    long raw = total / 6 + overdue * 4 + blocking * 5 + secrecy * 3 + hearing * 2;
    return (int) Math.min(raw, 100L);
  }

  private static long longValue(Object[] row, int index) {
    if (row == null || index < 0 || index >= row.length || row[index] == null) {
      return 0L;
    }
    return ((Number) row[index]).longValue();
  }

  private static String stringValue(Object[] row, int index, String fallback) {
    if (row == null || index < 0 || index >= row.length || row[index] == null) {
      return fallback;
    }
    String value = row[index].toString().trim();
    return value.isBlank() ? fallback : value;
  }

  private static String normalizeAxis(String raw) {
    if (raw == null || raw.isBlank()) {
      return "BASE";
    }
    return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
  }
}
