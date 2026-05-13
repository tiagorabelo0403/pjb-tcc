package com.tcc.pjb.backend.model.dto.admin;

import java.time.OffsetDateTime;
import java.util.List;




public record RitoReportResponse(
        OffsetDateTime generatedAt,
        int windowDays,
        double confidenceThreshold,
        List<RitoLowConfidenceStatDto> lowConfidenceByResolved,
        List<RitoMostCorrectedProcessDto> mostCorrectedProcesses,
        List<RitoSuggestionDto> topSuggestions
) {
    public double threshold() { return confidenceThreshold(); }
}

