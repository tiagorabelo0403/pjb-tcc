package com.tcc.pjb.backend.service.secretariat.triage;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;

@Component
public class SecretariatWorkloadBalancer {

  private static final List<String> ACTIVE_STATUSES = List.of("PENDENTE", "EM_EXECUCAO");

  private final SecretariatQueueItemRepository repository;

  public SecretariatWorkloadBalancer(SecretariatQueueItemRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  public SecretariatWorkloadProfile resolve(String inboxKey, TriageRoutingProfile triageProfile) {
    Objects.requireNonNull(inboxKey, "inboxKey");
    Objects.requireNonNull(triageProfile, "triageProfile");

    Object[] stats = repository.workload(inboxKey, ACTIVE_STATUSES, Instant.now());
    int active = asInt(stats, 0);
    int overdue = asInt(stats, 1);
    int expedited = asInt(stats, 2);

    String band = resolveBand(active, overdue, expedited, triageProfile);
    boolean fastTrackDesk = triageProfile.immediatePath() || overdue >= 18 || expedited >= 14;
    boolean rebalanceRequired = fastTrackDesk || "SATURADA".equals(band) || "CRITICA".equals(band);
    int effectivePriority = resolvePriority(triageProfile.priority(), band, triageProfile);
    Duration effectiveDue = resolveDue(triageProfile.dueIn(), band, triageProfile);
    String effectiveQueueCode = resolveQueueCode(triageProfile.queueCode(), band, triageProfile);
    String deskSuffix = resolveDeskSuffix(band, triageProfile);
    String descriptor = effectiveQueueCode + ':' + band + ':' + deskSuffix + ':' + active + ':' + overdue + ':' + expedited;

    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("baseInboxKey", inboxKey);
    metadata.put("activeItems", active);
    metadata.put("overdueItems", overdue);
    metadata.put("expeditedItems", expedited);
    metadata.put("workloadBand", band);
    metadata.put("rebalanceRequired", rebalanceRequired);
    metadata.put("fastTrackDesk", fastTrackDesk);
    metadata.put("effectivePriority", effectivePriority);
    metadata.put("effectiveQueueCode", effectiveQueueCode);
    metadata.put("deskSuffix", deskSuffix);
    metadata.put("effectiveDueHours", effectiveDue.toHours());
    metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

    return new SecretariatWorkloadProfile(
        active,
        overdue,
        expedited,
        band,
        rebalanceRequired,
        fastTrackDesk,
        effectivePriority,
        effectiveDue,
        effectiveQueueCode,
        deskSuffix,
        descriptor,
        metadata
    );
  }

  private static String resolveBand(int active, int overdue, int expedited, TriageRoutingProfile triageProfile) {
    if (active >= 220 || overdue >= 40 || expedited >= 30) {
      return "CRITICA";
    }
    if (active >= 120 || overdue >= 20 || expedited >= 15) {
      return "SATURADA";
    }
    if (active >= 60 || overdue >= 8 || expedited >= 6 || triageProfile.escalationRequired()) {
      return "PRESSAO";
    }
    if (active >= 20 || overdue >= 2) {
      return "EQUILIBRADA";
    }
    return "LIVRE";
  }

  private static int resolvePriority(int basePriority, String band, TriageRoutingProfile triageProfile) {
    if (triageProfile.blocking() || triageProfile.immediatePath()) {
      return 1;
    }
    int value = Math.max(1, Math.min(5, basePriority));
    if ("CRITICA".equals(band) || "SATURADA".equals(band)) {
      value = Math.max(1, value - 1);
    }
    if (triageProfile.hearingSensitive() && value > 2) {
      value--;
    }
    return Math.max(1, value);
  }

  private static Duration resolveDue(Duration baseDue, String band, TriageRoutingProfile triageProfile) {
    Duration base = baseDue == null || baseDue.isNegative() || baseDue.isZero() ? Duration.ofHours(4) : baseDue;
    if (triageProfile.blocking()) {
      return min(base, Duration.ofMinutes(20));
    }
    if (triageProfile.immediatePath()) {
      return min(base, Duration.ofHours(1));
    }
    if ("CRITICA".equals(band)) {
      return min(base, Duration.ofHours(2));
    }
    if ("SATURADA".equals(band)) {
      return min(base, Duration.ofHours(4));
    }
    return base;
  }

  private static String resolveQueueCode(String base, String band, TriageRoutingProfile triageProfile) {
    String prefix = base == null || base.isBlank() ? "SECRETARIA" : base.trim();
    if (triageProfile.blocking()) {
      return prefix + ":BLOQUEANTE";
    }
    if (triageProfile.secrecyReviewRequired()) {
      return prefix + ":SIGILO";
    }
    if (triageProfile.hearingSensitive()) {
      return prefix + ":AUDIENCIA";
    }
    return switch (band) {
      case "CRITICA" -> prefix + ":CRITICA";
      case "SATURADA" -> prefix + ":SATURADA";
      case "PRESSAO" -> prefix + ":PRESSAO";
      default -> prefix;
    };
  }

  private static String resolveDeskSuffix(String band, TriageRoutingProfile triageProfile) {
    if (triageProfile.blocking()) {
      return "FAST_TRACK";
    }
    if (triageProfile.secrecyReviewRequired()) {
      return "SIGILO";
    }
    if (triageProfile.hearingSensitive()) {
      return "AUDIENCIA";
    }
    return switch (band) {
      case "CRITICA" -> "ESCALONADA";
      case "SATURADA" -> "REDISTRIBUIR";
      case "PRESSAO" -> "MONITORAR";
      default -> "PADRAO";
    };
  }

  private static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }

  private static int asInt(Object[] stats, int index) {
    if (stats == null || index < 0 || index >= stats.length || stats[index] == null) {
      return 0;
    }
    return ((Number) stats[index]).intValue();
  }
}
