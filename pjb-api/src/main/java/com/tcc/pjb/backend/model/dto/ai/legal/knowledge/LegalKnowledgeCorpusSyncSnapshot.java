package com.tcc.pjb.backend.model.dto.ai.legal.knowledge;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalKnowledgeCorpusSyncSnapshot(
        String status,
        Instant executedAt,
        int totalSources,
        int changedSources,
        int totalArtifacts,
        List<String> changedSourceIds,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("executedAt", executedAt);
        out.put("totalSources", totalSources);
        out.put("changedSources", changedSources);
        out.put("totalArtifacts", totalArtifacts);
        out.put("changedSourceIds", changedSourceIds == null ? List.of() : List.copyOf(changedSourceIds));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
