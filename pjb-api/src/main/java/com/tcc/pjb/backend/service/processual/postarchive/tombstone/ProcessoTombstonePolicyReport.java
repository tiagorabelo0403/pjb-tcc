package com.tcc.pjb.backend.service.processual.postarchive.tombstone;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessoTombstonePolicyReport(
        ProcessoTombstoneStatus status,
        boolean archived,
        boolean visiblePanel,
        boolean hiddenByArchivePolicy,
        boolean reexposedBySecretariat,
        boolean controlledAccessRequired,
        int hideAfterDays,
        long archiveAgeDays,
        List<String> alerts,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status != null ? status.name() : null);
        out.put("archived", archived);
        out.put("visiblePanel", visiblePanel);
        out.put("hiddenByArchivePolicy", hiddenByArchivePolicy);
        out.put("reexposedBySecretariat", reexposedBySecretariat);
        out.put("controlledAccessRequired", controlledAccessRequired);
        out.put("hideAfterDays", hideAfterDays);
        out.put("archiveAgeDays", archiveAgeDays);
        out.put("alerts", alerts == null ? List.of() : alerts);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
