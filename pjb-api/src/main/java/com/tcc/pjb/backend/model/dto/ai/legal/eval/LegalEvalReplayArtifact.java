package com.tcc.pjb.backend.model.dto.ai.legal.eval;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalEvalReplayArtifact(
        String replayId,
        String suiteId,
        String capability,
        String version,
        String selectionMode,
        List<String> pinnedServers,
        List<String> safeguards,
        Map<String, Object> context
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("replayId", replayId);
        out.put("suiteId", suiteId);
        out.put("capability", capability);
        out.put("version", version);
        out.put("selectionMode", selectionMode);
        out.put("pinnedServers", pinnedServers == null ? List.of() : List.copyOf(pinnedServers));
        out.put("safeguards", safeguards == null ? List.of() : List.copyOf(safeguards));
        out.put("context", context == null ? Map.of() : Map.copyOf(context));
        return Collections.unmodifiableMap(out);
    }
}
