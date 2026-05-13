package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.Instant;
import java.util.List;

public record RitoPlanResponse(
        String requestId,
        Instant generatedAt,
        String rito,
        String packVersion,
        String packChecksum,
        boolean packLoaded,
        List<String> packIssues,
        List<RitoStageDto> stages
) {

    public record RitoStageDto(
            String fase,
            List<String> allowedNext,
            List<WorkDto> work
    ) {}

    public record WorkDto(
            String code,
            String type,
            String title,
            String description,
            String actorRole,
            Integer priority,
            Integer slaDays,
            Boolean blocking,
            List<String> legalBases
    ) {}
}
