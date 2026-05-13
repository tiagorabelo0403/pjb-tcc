package com.tcc.pjb.backend.model.dto.institutional.support.operations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstitutionalSupportCompetenceSnapshotResponse(
        Instant generatedAt,
        Map<String, Object> lane,
        Map<String, Object> metrics,
        List<Rule> rules,
        List<String> warnings,
        Map<String, Object> routes
) {
    public record Rule(
            String actCode,
            String actLabel,
            String actAxis,
            String minimumRole,
            String delegatedFunction,
            String ramoAxis,
            String ritoAxis,
            String phaseAxis,
            String secrecyAxis,
            boolean functionalCredentialRequired,
            List<String> compatibleScopes,
            List<String> signals
    ) {
    }
}
