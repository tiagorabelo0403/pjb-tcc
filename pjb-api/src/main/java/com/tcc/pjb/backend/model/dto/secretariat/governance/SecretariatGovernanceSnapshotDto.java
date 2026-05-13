package com.tcc.pjb.backend.model.dto.secretariat.governance;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecretariatGovernanceSnapshotDto(
        Instant generatedAt,
        String inboxKey,
        String inboxDescriptor,
        Map<String, Object> context,
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
            String ritoAxis,
            String ramoAxis,
            String phaseAxis,
            String secrecyAxis,
            String urgencyAxis,
            boolean functionalCredentialRequired,
            List<String> compatibleCategories,
            List<String> institutionalScopes,
            List<String> signals
    ) {
    }
}
