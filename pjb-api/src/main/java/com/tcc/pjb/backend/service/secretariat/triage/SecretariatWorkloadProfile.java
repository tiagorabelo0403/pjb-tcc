package com.tcc.pjb.backend.service.secretariat.triage;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SecretariatWorkloadProfile(
    int activeItems,
    int overdueItems,
    int expeditedItems,
    String workloadBand,
    boolean rebalanceRequired,
    boolean fastTrackDesk,
    int effectivePriority,
    Duration effectiveDueIn,
    String effectiveQueueCode,
    String deskSuffix,
    String descriptor,
    LinkedHashMap<String, Object> metadata
) {

  public SecretariatWorkloadProfile {
    activeItems = Math.max(0, activeItems);
    overdueItems = Math.max(0, overdueItems);
    expeditedItems = Math.max(0, expeditedItems);
    workloadBand = normalizeBand(workloadBand);
    effectivePriority = Math.max(1, Math.min(5, effectivePriority));
    effectiveDueIn = effectiveDueIn == null || effectiveDueIn.isNegative() || effectiveDueIn.isZero() ? Duration.ofHours(4) : effectiveDueIn;
    effectiveQueueCode = blankToNull(effectiveQueueCode);
    deskSuffix = blankToNull(deskSuffix);
    descriptor = blankToNull(descriptor);
    metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
  }

  public boolean saturated() {
    return "SATURADA".equals(workloadBand) || "CRITICA".equals(workloadBand);
  }

  public boolean pressured() {
    return saturated() || "PRESSAO".equals(workloadBand);
  }

  public String fingerprint() {
    return workloadBand + ':' + effectivePriority + ':' + normalizedToken(effectiveQueueCode) + ':' + normalizedToken(deskSuffix);
  }

  public Map<String, Object> toMap() {
    LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
    out.put("activeItems", activeItems);
    out.put("overdueItems", overdueItems);
    out.put("expeditedItems", expeditedItems);
    out.put("workloadBand", workloadBand);
    out.put("rebalanceRequired", rebalanceRequired);
    out.put("fastTrackDesk", fastTrackDesk);
    out.put("effectivePriority", effectivePriority);
    out.put("effectiveDueHours", effectiveDueIn.toHours());
    out.put("effectiveQueueCode", effectiveQueueCode);
    out.put("deskSuffix", deskSuffix);
    out.put("descriptor", descriptor);
    out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
    return Collections.unmodifiableMap(out);
  }

  private static String normalizeBand(String raw) {
    String value = blankToNull(raw);
    if (value == null) {
      return "BASE";
    }
    String normalized = value.trim().toUpperCase();
    return switch (normalized) {
      case "BASE", "LIVRE", "EQUILIBRADA", "PRESSAO", "SATURADA", "CRITICA" -> normalized;
      default -> "BASE";
    };
  }

  private static String normalizedToken(String raw) {
    String value = blankToNull(raw);
    return value == null ? "BASE" : value.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
