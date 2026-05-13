package com.tcc.pjb.backend.service.secretariat.triage;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record TriageRoutingProfile(
    UrgencyLevel level,
    int score,
    int priority,
    Duration dueIn,
    String inboxKey,
    String queueCode,
    String deskAxis,
    String workTitle,
    String descriptor,
    boolean escalationRequired,
    boolean secrecyReviewRequired,
    boolean hearingSensitive,
    boolean blocking,
    List<String> tags,
    LinkedHashMap<String, Object> metadata
) {

  public TriageRoutingProfile {
    if (score < 0 || score > 100) {
      throw new IllegalArgumentException("score out of range");
    }
    if (priority < 1 || priority > 5) {
      throw new IllegalArgumentException("priority out of range");
    }
    dueIn = dueIn == null ? Duration.ofDays(1) : dueIn;
    inboxKey = blankToNull(inboxKey);
    queueCode = blankToNull(queueCode);
    deskAxis = blankToNull(deskAxis);
    workTitle = blankToNull(workTitle);
    descriptor = blankToNull(descriptor);
    tags = tags == null ? List.of() : List.copyOf(tags);
    metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
  }

  public boolean immediatePath() {
    return priority <= 2 || escalationRequired || blocking;
  }

  public boolean specializedPath() {
    return deskAxis != null && !"COMUM".equalsIgnoreCase(deskAxis);
  }

  public String queueBucket() {
    return firstNonBlank(queueCode, deskAxis, level != null ? level.name() : null, "SECRETARIA");
  }

  public Map<String, Object> toMap() {
    LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
    out.put("level", level != null ? level.name() : null);
    out.put("score", score);
    out.put("priority", priority);
    out.put("dueHours", dueIn.toHours());
    out.put("inboxKey", inboxKey);
    out.put("queueCode", queueCode);
    out.put("deskAxis", deskAxis);
    out.put("workTitle", workTitle);
    out.put("descriptor", descriptor);
    out.put("escalationRequired", escalationRequired);
    out.put("secrecyReviewRequired", secrecyReviewRequired);
    out.put("hearingSensitive", hearingSensitive);
    out.put("blocking", blocking);
    out.put("tags", tags);
    out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
    return Collections.unmodifiableMap(out);
  }

  public String fingerprint() {
    return String.join(":",
        normalizeToken(queueBucket()),
        normalizeToken(deskAxis),
        normalizeToken(level != null ? level.name() : null),
        normalizeToken(descriptor));
  }

  private static String normalizeToken(String raw) {
    if (raw == null || raw.isBlank()) {
      return "BASE";
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("(^_|_$)", "");
    return normalized.isBlank() ? "BASE" : normalized;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}
