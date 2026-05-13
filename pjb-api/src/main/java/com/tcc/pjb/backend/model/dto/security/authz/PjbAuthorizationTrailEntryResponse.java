package com.tcc.pjb.backend.model.dto.security.authz;

import java.time.Instant;

public record PjbAuthorizationTrailEntryResponse(
        Instant occurredAt,
        String auditEventCode,
        String action,
        String resourceType,
        String resourceId,
        boolean allowed,
        String reason,
        String policyVersion,
        String actorType,
        Long actorId,
        String requestId,
        String justificativa,
        String effectiveSigilo,
        String riskLevel,
        int riskScore,
        boolean stepUpRequired,
        boolean stepUpSatisfied,
        String stepUpChannel,
        String stepUpCode,
        boolean governanceRequired,
        boolean governanceSatisfied,
        String governanceChannel,
        String governanceCode,
        String governanceScope,
        String integrationCode,
        String institutionalUnitCode,
        String institutionalBoxCode,
        String institutionalCapabilityCode,
        String expedicaoUuid,
        String payloadHash,
        String auditDescription
) {
}
