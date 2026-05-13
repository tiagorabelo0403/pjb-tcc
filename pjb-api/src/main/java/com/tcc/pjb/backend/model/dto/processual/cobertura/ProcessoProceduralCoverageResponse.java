package com.tcc.pjb.backend.model.dto.processual.cobertura;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessoProceduralCoverageResponse(
        Instant generatedAt,
        boolean supportsAllBrazilianRites,
        boolean supportsAllBrazilianRights,
        boolean supportsAllProceduralGuarantees,
        int totalRitos,
        int totalRamos,
        int totalGrupos,
        List<String> justiceTracks,
        List<String> constitutionalGuarantees,
        List<ProcessoProceduralCoverageFamilyResponse> familyCoverage,
        List<ProcessoProceduralGuaranteeResponse> ritoCoverage,
        Map<String, Object> metadata
) {
}
