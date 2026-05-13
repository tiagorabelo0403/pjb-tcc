package com.tcc.pjb.backend.core.security.abac;

import java.time.Instant;
import java.util.Locale;

public record PjbAuthorizationTrailQueryCriteria(
        String actionPrefix,
        String resourceType,
        String resourceId,
        Long actorId,
        String actorType,
        Boolean allowed,
        PjbAuthorizationRiskLevel riskLevel,
        String requestId,
        String integrationCode,
        String institutionalUnitCode,
        String institutionalBoxCode,
        String institutionalCapabilityCode,
        String governanceChannel,
        String governanceScope,
        Boolean governanceRequired,
        Boolean governanceSatisfied,
        Boolean stepUpRequired,
        Boolean stepUpSatisfied,
        Instant occurredAfter,
        Instant occurredBefore,
        int limit
) {

    public PjbAuthorizationTrailQueryCriteria {
        actionPrefix = normalize(actionPrefix);
        resourceType = normalize(resourceType);
        resourceId = normalize(resourceId);
        actorType = normalize(actorType);
        requestId = normalize(requestId);
        integrationCode = normalize(integrationCode);
        institutionalUnitCode = normalize(institutionalUnitCode);
        institutionalBoxCode = normalize(institutionalBoxCode);
        institutionalCapabilityCode = normalize(institutionalCapabilityCode);
        governanceChannel = normalize(governanceChannel);
        governanceScope = normalize(governanceScope);
        limit = Math.min(5000, Math.max(1, limit <= 0 ? 100 : limit));
    }

    public static PjbAuthorizationTrailQueryCriteria defaults() {
        return new PjbAuthorizationTrailQueryCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                100
        );
    }

    public boolean matches(PjbAuthorizationTrailSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (actionPrefix != null && !snapshot.action().startsWith(actionPrefix)) {
            return false;
        }
        if (resourceType != null && !resourceType.equals(snapshot.resourceType())) {
            return false;
        }
        if (resourceId != null && !resourceId.equals(snapshot.resourceId())) {
            return false;
        }
        if (actorId != null && !actorId.equals(snapshot.actorId())) {
            return false;
        }
        if (actorType != null && !actorType.equals(snapshot.actorType())) {
            return false;
        }
        if (allowed != null && allowed.booleanValue() != snapshot.allowed()) {
            return false;
        }
        if (riskLevel != null && riskLevel != snapshot.riskLevel()) {
            return false;
        }
        if (requestId != null && !requestId.equals(snapshot.requestId())) {
            return false;
        }
        if (integrationCode != null && !integrationCode.equals(snapshot.integrationCode())) {
            return false;
        }
        if (institutionalUnitCode != null && !institutionalUnitCode.equals(snapshot.institutionalUnitCode())) {
            return false;
        }
        if (institutionalBoxCode != null && !institutionalBoxCode.equals(snapshot.institutionalBoxCode())) {
            return false;
        }
        if (institutionalCapabilityCode != null && !institutionalCapabilityCode.equals(snapshot.institutionalCapabilityCode())) {
            return false;
        }
        if (governanceChannel != null && !governanceChannel.equals(snapshot.governanceChannel())) {
            return false;
        }
        if (governanceScope != null && !governanceScope.equals(snapshot.governanceScope())) {
            return false;
        }
        if (governanceRequired != null && governanceRequired.booleanValue() != snapshot.governanceRequired()) {
            return false;
        }
        if (governanceSatisfied != null && governanceSatisfied.booleanValue() != snapshot.governanceSatisfied()) {
            return false;
        }
        if (stepUpRequired != null && stepUpRequired.booleanValue() != snapshot.stepUpRequired()) {
            return false;
        }
        if (stepUpSatisfied != null && stepUpSatisfied.booleanValue() != snapshot.stepUpSatisfied()) {
            return false;
        }
        if (occurredAfter != null && snapshot.occurredAt().isBefore(occurredAfter)) {
            return false;
        }
        if (occurredBefore != null && snapshot.occurredAt().isAfter(occurredBefore)) {
            return false;
        }
        return true;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
