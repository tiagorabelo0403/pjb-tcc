package com.tcc.pjb.backend.model.dto.ai.legal.knowledge;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalKnowledgeCorpusSourceView(
        String sourceId,
        String title,
        String sourceKind,
        String authorityLevel,
        String institution,
        String storageLane,
        String licensingModel,
        String refreshStrategy,
        String versionTag,
        boolean officialSource,
        boolean doctrineSource,
        boolean active,
        int artifactCount,
        int revisionCount,
        Instant lastSyncedAt,
        Instant nextRefreshAt,
        List<String> branches,
        List<String> artifactTypes,
        List<String> retrievalTags,
        List<String> restrictions,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("sourceId", sourceId);
        out.put("title", title);
        out.put("sourceKind", sourceKind);
        out.put("authorityLevel", authorityLevel);
        out.put("institution", institution);
        out.put("storageLane", storageLane);
        out.put("licensingModel", licensingModel);
        out.put("refreshStrategy", refreshStrategy);
        out.put("versionTag", versionTag);
        out.put("officialSource", officialSource);
        out.put("doctrineSource", doctrineSource);
        out.put("active", active);
        out.put("artifactCount", artifactCount);
        out.put("revisionCount", revisionCount);
        out.put("lastSyncedAt", lastSyncedAt);
        out.put("nextRefreshAt", nextRefreshAt);
        out.put("branches", branches == null ? List.of() : List.copyOf(branches));
        out.put("artifactTypes", artifactTypes == null ? List.of() : List.copyOf(artifactTypes));
        out.put("retrievalTags", retrievalTags == null ? List.of() : List.copyOf(retrievalTags));
        out.put("restrictions", restrictions == null ? List.of() : List.copyOf(restrictions));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
