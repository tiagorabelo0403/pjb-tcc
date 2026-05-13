package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse(
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
