package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NationalProceduralTribunalVariationSnapshot(
        Instant generatedAt,
        int totalTribunais,
        List<NationalProceduralTribunalVariationRow> rows,
        Map<String, Object> metadata
) {
    public NationalProceduralTribunalVariationSnapshot {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        rows = NationalProceduralRecordSupport.copyList(rows);
        metadata = NationalProceduralRecordSupport.copyMap(metadata);
    }
}
