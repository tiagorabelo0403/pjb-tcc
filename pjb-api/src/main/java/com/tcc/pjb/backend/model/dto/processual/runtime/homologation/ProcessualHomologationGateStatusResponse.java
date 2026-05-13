package com.tcc.pjb.backend.model.dto.processual.runtime.homologation;

import java.time.Instant;
import java.util.List;

public record ProcessualHomologationGateStatusResponse(
        Long processoId,
        String operationCode,
        boolean operationSensivel,
        boolean blocked,
        List<String> blockerCodes,
        List<ProcessualHomologationBlockerDetailResponse> details,
        String hashIntegridade,
        Instant evaluatedAt
) {
    public ProcessualHomologationGateStatusResponse {
        blockerCodes = blockerCodes == null ? List.of() : List.copyOf(blockerCodes);
        details = details == null ? List.of() : List.copyOf(details);
        evaluatedAt = evaluatedAt == null ? Instant.now() : evaluatedAt;
    }
}
