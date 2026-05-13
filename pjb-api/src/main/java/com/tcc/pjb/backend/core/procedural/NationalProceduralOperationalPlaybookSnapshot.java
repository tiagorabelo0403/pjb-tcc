package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NationalProceduralOperationalPlaybookSnapshot(
        Instant generatedAt,
        boolean supportsAllBrazilianRites,
        boolean supportsAllProceduralCompetenceTracks,
        int totalRitos,
        int totalFamilies,
        List<String> competenceTracks,
        List<NationalProceduralOperationalPlaybookRow> rows,
        Map<String, Object> metadata
) {
    public NationalProceduralOperationalPlaybookSnapshot {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        competenceTracks = NationalProceduralRecordSupport.copyList(competenceTracks);
        rows = NationalProceduralRecordSupport.copyList(rows);
        metadata = NationalProceduralRecordSupport.copyMap(metadata);
    }
}
