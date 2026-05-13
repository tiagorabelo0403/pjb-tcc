package com.tcc.pjb.backend.service.secretariat.query.queue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SecretariatQueueLoadProfile(
    String inboxKey,
    long totalItems,
    long overdueItems,
    long criticalItems,
    long dueWithin24hItems,
    String loadBand,
    String responseMode,
    boolean rebalanceSuggested,
    List<String> markers,
    LinkedHashMap<String, Object> metadata
) {

  public SecretariatQueueLoadProfile {
    markers = markers == null ? List.of() : List.copyOf(markers);
    metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
  }

  public boolean hasPressure() {
    return overdueItems > 0 || criticalItems > 0 || "HIGH".equals(loadBand) || "SATURATED".equals(loadBand);
  }

  public String descriptor() {
    return firstNonBlank(loadBand, "NORMAL") + ':' + firstNonBlank(responseMode, "FLOW_STANDARD") + ':' + totalItems;
  }

  public Map<String, Object> toMap() {
    LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
    out.put("inboxKey", inboxKey);
    out.put("totalItems", totalItems);
    out.put("overdueItems", overdueItems);
    out.put("criticalItems", criticalItems);
    out.put("dueWithin24hItems", dueWithin24hItems);
    out.put("loadBand", loadBand);
    out.put("responseMode", responseMode);
    out.put("rebalanceSuggested", rebalanceSuggested);
    out.put("markers", markers);
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
