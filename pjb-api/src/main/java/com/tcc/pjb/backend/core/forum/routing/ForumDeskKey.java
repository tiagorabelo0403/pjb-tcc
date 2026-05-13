package com.tcc.pjb.backend.core.forum.routing;

import java.util.Locale;
import java.util.Objects;

public record ForumDeskKey(
    String inboxKey,
    JudicialOrganRef organ,
    ForumInstance instance,
    ForumLane lane,
    String uf,
    String comarca,
    String unitHint
) {

  public ForumDeskKey {
    Objects.requireNonNull(inboxKey, "inboxKey");
    Objects.requireNonNull(organ, "organ");
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(lane, "lane");
    inboxKey = inboxKey.trim();
    if (inboxKey.isEmpty() || inboxKey.length() > 120) {
      throw new IllegalArgumentException("inboxKey inválida");
    }
    uf = normalizeRegionToken(uf, "XX");
    comarca = comarca == null ? "" : comarca.trim();
    unitHint = unitHint == null ? "" : unitHint.trim();
  }

  public boolean isSecondInstance() {
    return instance == ForumInstance.SECOND || instance == ForumInstance.SUPERIOR;
  }

  public boolean isSpecializedDesk() {
    return lane.isSpecialized() || organ.kind() == JudicialOrganKind.TRF || organ.kind() == JudicialOrganKind.TRT
        || organ.kind() == JudicialOrganKind.TRE || organ.kind() == JudicialOrganKind.TJM;
  }

  public String territorialLabel() {
    return firstNonBlank(comarca, uf, unitHint, organ.displayName());
  }

  public String descriptor() {
    return organ.code() + '/' + instance.name() + '/' + lane.token() + '/' + territorialLabel();
  }

  public String normalizedUnitHint() {
    return normalizeRegionToken(unitHint, "UNIDADE_BASE");
  }

  public boolean matchesLaneToken(String raw) {
    return raw != null && normalizeRegionToken(raw, "").equals(lane.token());
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

  private static String normalizeRegionToken(String raw, String fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("(^_|_$)", "");
    return normalized.isBlank() ? fallback : normalized;
  }
}
