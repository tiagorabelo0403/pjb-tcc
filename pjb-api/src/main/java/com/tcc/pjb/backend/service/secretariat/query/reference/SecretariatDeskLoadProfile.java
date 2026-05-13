package com.tcc.pjb.backend.service.secretariat.query.reference;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SecretariatDeskLoadProfile(
    String inboxKey,
    String dominantDeskAxis,
    long totalItems,
    long overdueItems,
    long blockingItems,
    long secrecyReviewItems,
    long hearingSensitiveItems,
    String loadBand,
    String redistributionDesk,
    String gabineteSupportDesk,
    String coordinationMode,
    boolean forceRedistribution,
    boolean secrecyPressure,
    boolean hearingPressure,
    List<String> labels,
    LinkedHashMap<String, Object> metadata
) {

  public SecretariatDeskLoadProfile {
    labels = labels == null ? List.of() : List.copyOf(labels);
    metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
  }

  public String descriptor() {
    return firstNonBlank(dominantDeskAxis, "BASE") + ':'
        + firstNonBlank(loadBand, "NORMAL") + ':'
        + firstNonBlank(coordinationMode, "FLOW_STANDARD") + ':'
        + totalItems;
  }

  public Map<String, Object> toMap() {
    LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
    out.put("inboxKey", inboxKey);
    out.put("dominantDeskAxis", dominantDeskAxis);
    out.put("totalItems", totalItems);
    out.put("overdueItems", overdueItems);
    out.put("blockingItems", blockingItems);
    out.put("secrecyReviewItems", secrecyReviewItems);
    out.put("hearingSensitiveItems", hearingSensitiveItems);
    out.put("loadBand", loadBand);
    out.put("redistributionDesk", redistributionDesk);
    out.put("gabineteSupportDesk", gabineteSupportDesk);
    out.put("coordinationMode", coordinationMode);
    out.put("forceRedistribution", forceRedistribution);
    out.put("secrecyPressure", secrecyPressure);
    out.put("hearingPressure", hearingPressure);
    out.put("labels", labels);
    out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
    return Collections.unmodifiableMap(out);
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
