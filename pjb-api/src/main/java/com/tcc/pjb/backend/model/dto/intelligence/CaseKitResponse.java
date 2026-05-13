package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CaseKitResponse(
        String requestId,
        Instant generatedAt,
        CaseTriageResponse triage,
        RitoPlanResponse ritoPlan,
        MaterialKitDto material,
        Map<String, Object> debug
) {

    public record MaterialKitDto(
            List<String> requiredDocuments,
            List<String> proofChecklist,
            List<String> legalBases,
            List<String> warnings
    ) {}
}
