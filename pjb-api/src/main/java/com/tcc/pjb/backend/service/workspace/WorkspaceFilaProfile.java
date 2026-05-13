package com.tcc.pjb.backend.service.workspace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WorkspaceFilaProfile(
    String descriptor,
    String operationalMode,
    String scope,
    Integer autoRefreshSeconds,
    String sortHint,
    String workloadBand,
    String assistantDesk,
    String escalationDesk,
    String coordinationChannel,
    boolean redistributionEligible,
    boolean audienceSensitive,
    List<String> labels,
    LinkedHashMap<String, Object> metadata
) {

  public WorkspaceFilaProfile {
    labels = labels == null ? List.of() : List.copyOf(labels);
    metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
  }

  public Map<String, Object> toMap() {
    LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
    out.put("descriptor", descriptor);
    out.put("operationalMode", operationalMode);
    out.put("scope", scope);
    out.put("autoRefreshSeconds", autoRefreshSeconds);
    out.put("sortHint", sortHint);
    out.put("workloadBand", workloadBand);
    out.put("assistantDesk", assistantDesk);
    out.put("escalationDesk", escalationDesk);
    out.put("coordinationChannel", coordinationChannel);
    out.put("redistributionEligible", redistributionEligible);
    out.put("audienceSensitive", audienceSensitive);
    out.put("labels", labels);
    out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
    return Collections.unmodifiableMap(out);
  }
}
