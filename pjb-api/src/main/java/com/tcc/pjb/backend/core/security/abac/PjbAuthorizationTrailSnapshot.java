package com.tcc.pjb.backend.core.security.abac;

import java.time.Instant;
import java.util.Locale;

public record PjbAuthorizationTrailSnapshot(
        Instant occurredAt,
        String auditEventCode,
        String action,
        String resourceType,
        String resourceId,
        boolean allowed,
        String reason,
        String policyVersion,
        String policyDescriptorSha256,
        Long actorId,
        String actorType,
        String requestId,
        String justificativa,
        String effectiveSigilo,
        PjbAuthorizationRiskLevel riskLevel,
        int riskScore,
        String stepUpChannel,
        String stepUpCode,
        boolean stepUpRequired,
        boolean stepUpSatisfied,
        String governanceChannel,
        String governanceCode,
        String governanceScope,
        boolean governanceRequired,
        boolean governanceSatisfied,
        String integrationCode,
        String institutionalUnitCode,
        String institutionalBoxCode,
        String institutionalCapabilityCode,
        String expedicaoUuid,
        String payloadHash,
        String auditDescription
) {

    public PjbAuthorizationTrailSnapshot {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        auditEventCode = normalize(auditEventCode);
        action = normalize(action);
        resourceType = normalize(resourceType);
        resourceId = normalize(resourceId);
        reason = sanitize(reason);
        policyVersion = normalize(policyVersion);
        policyDescriptorSha256 = normalize(policyDescriptorSha256);
        actorType = normalize(actorType);
        requestId = normalize(requestId);
        justificativa = sanitize(justificativa);
        effectiveSigilo = normalize(effectiveSigilo);
        riskLevel = riskLevel == null ? PjbAuthorizationRiskLevel.BAIXO : riskLevel;
        stepUpChannel = normalize(stepUpChannel);
        stepUpCode = normalize(stepUpCode);
        governanceChannel = normalize(governanceChannel);
        governanceCode = normalize(governanceCode);
        governanceScope = normalize(governanceScope);
        integrationCode = normalize(integrationCode);
        institutionalUnitCode = normalize(institutionalUnitCode);
        institutionalBoxCode = normalize(institutionalBoxCode);
        institutionalCapabilityCode = normalize(institutionalCapabilityCode);
        expedicaoUuid = normalize(expedicaoUuid);
        payloadHash = normalize(payloadHash);
        auditDescription = sanitize(auditDescription);
    }

    static PjbAuthorizationTrailSnapshot from(PjbAuthorizationDecisionTrail trail) {
        if (trail == null) {
            return null;
        }
        PjbAuthorizationStepUpAssessment stepUp = trail.stepUp();
        PjbAuthorizationGovernanceAssessment governance = trail.governance();
        ParsedInstitutionalResource institutional = ParsedInstitutionalResource.parse(trail.resourceType(), trail.resourceId());
        String integrationCode = "INTEGRACAO_EXTERNA".equalsIgnoreCase(trail.resourceType()) ? trail.resourceId() : null;
        return new PjbAuthorizationTrailSnapshot(
                Instant.now(),
                trail.auditEventCode(),
                trail.action(),
                trail.resourceType(),
                trail.resourceId(),
                trail.allowed(),
                trail.reason(),
                trail.policyVersion(),
                trail.policyDescriptorSha256(),
                trail.actorId(),
                trail.actorType(),
                trail.requestId(),
                trail.justificativa(),
                trail.effectiveSigilo() == null ? null : trail.effectiveSigilo().name(),
                trail.riskLevel(),
                trail.riskScore(),
                stepUp == null ? null : stepUp.channel(),
                stepUp == null ? null : stepUp.code(),
                stepUp != null && stepUp.required(),
                stepUp != null && stepUp.satisfied(),
                governance == null ? null : governance.channel(),
                governance == null ? null : governance.code(),
                governance == null ? null : governance.scope(),
                governance != null && governance.required(),
                governance != null && governance.satisfied(),
                integrationCode,
                institutional.unidadeCodigo(),
                institutional.caixaCodigo(),
                institutional.capacidadeCodigo(),
                institutional.expedicaoUuid(),
                trail.payloadHash(),
                trail.auditDescription()
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "NONE";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private record ParsedInstitutionalResource(String unidadeCodigo,
                                               String caixaCodigo,
                                               String capacidadeCodigo,
                                               String expedicaoUuid) {

        static ParsedInstitutionalResource parse(String resourceType, String resourceId) {
            if (!"CAIXA_INSTITUCIONAL".equalsIgnoreCase(resourceType) || resourceId == null || resourceId.isBlank()) {
                return new ParsedInstitutionalResource(null, null, null, null);
            }
            String[] parts = resourceId.split(":", -1);
            String unidade = parts.length > 0 ? parts[0] : null;
            String caixa = parts.length > 1 ? parts[1] : null;
            String capacidade = parts.length > 2 ? parts[2] : null;
            String expedicao = parts.length > 3 ? parts[3] : null;
            return new ParsedInstitutionalResource(unidade, caixa, capacidade, expedicao);
        }
    }
}
