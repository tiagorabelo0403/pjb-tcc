package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalIntegrationSecurityPolicy(
        String targetCode,
        String targetType,
        String organizationScope,
        String displayName,
        String trustFloor,
        List<String> enabledChannels,
        List<String> integrationFamilies,
        boolean requiresMutualTls,
        boolean requiresPayloadSignature,
        boolean requiresOriginAllowlist,
        boolean requiresImmediateRevocation,
        boolean requiresHumanApproval,
        int credentialRotationDays,
        List<String> mandatoryControls,
        List<String> fundamentos,
        Instant generatedAt
) {
}
