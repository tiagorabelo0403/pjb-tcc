package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NationalProceduralRightsCoverageSnapshot(
        Instant generatedAt,
        boolean supportsAllBrazilianRites,
        boolean supportsAllBrazilianRights,
        boolean supportsAllProceduralGuarantees,
        int totalRitos,
        int totalRamos,
        int totalGrupos,
        List<String> justiceTracks,
        List<String> constitutionalGuarantees,
        List<NationalProceduralRightsCoverageFamily> familyCoverage,
        List<NationalProceduralRightsCoverageRow> ritoCoverage,
        Map<String, Object> metadata
) {
}
