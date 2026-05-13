package com.tcc.pjb.backend.service.processual.postarchive.visibility;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ArchivedProcessVisibilityPolicyReport(
        boolean eligible,
        ArchivedProcessVisibilityMode mode,
        int suggestedHideAfterDays,
        boolean controlledAccessRequired,
        boolean partyAuthorizationPreferred,
        List<String> allowedRequesterProfiles,
        List<String> alerts,
        Map<String, Object> metadata
) {
    public ArchivedProcessVisibilityPolicyReport {
        allowedRequesterProfiles = allowedRequesterProfiles == null ? List.of() : List.copyOf(allowedRequesterProfiles);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("eligible", eligible);
        out.put("mode", mode != null ? mode.name() : null);
        out.put("suggestedHideAfterDays", suggestedHideAfterDays);
        out.put("controlledAccessRequired", controlledAccessRequired);
        out.put("partyAuthorizationPreferred", partyAuthorizationPreferred);
        out.put("allowedRequesterProfiles", allowedRequesterProfiles);
        out.put("alerts", alerts);
        out.put("metadata", metadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
