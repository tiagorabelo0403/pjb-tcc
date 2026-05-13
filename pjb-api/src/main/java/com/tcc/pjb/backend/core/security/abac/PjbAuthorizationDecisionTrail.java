package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Locale;

public record PjbAuthorizationDecisionTrail(
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
        NivelSigilo effectiveSigilo,
        PjbAuthorizationRiskLevel riskLevel,
        int riskScore,
        PjbAuthorizationStepUpAssessment stepUp,
        PjbAuthorizationGovernanceAssessment governance
) {

    public String auditEventCode() {
        return "AUTHZ_" + normalize(action) + '_' + (allowed ? "ALLOW" : "DENY");
    }

    public String payloadHash() {
        return Hashes.sha256Hex(String.join("|",
                normalize(action),
                normalize(resourceType),
                normalize(resourceId),
                String.valueOf(allowed),
                normalize(reason),
                normalize(policyVersion),
                normalize(policyDescriptorSha256),
                String.valueOf(actorId),
                normalize(actorType),
                normalize(requestId),
                normalize(justificativa),
                effectiveSigilo == null ? "PUBLICO" : effectiveSigilo.name(),
                riskLevel == null ? PjbAuthorizationRiskLevel.BAIXO.name() : riskLevel.name(),
                String.valueOf(riskScore),
                stepUp == null ? "NO_STEP_UP" : normalize(stepUp.channel()) + ':' + normalize(stepUp.code()) + ':' + stepUp.required() + ':' + stepUp.satisfied(),
                governance == null ? "NO_GOVERNANCE" : normalize(governance.channel()) + ':' + normalize(governance.code()) + ':' + normalize(governance.scope()) + ':' + governance.required() + ':' + governance.satisfied()
        ));
    }

    public String auditDescription() {
        return String.join("; ",
                "action=" + normalize(action),
                "resource=" + normalize(resourceType) + ':' + normalize(resourceId),
                "allowed=" + allowed,
                "reason=" + normalize(reason),
                "policy=" + normalize(policyVersion),
                "actor=" + normalize(actorType) + ':' + String.valueOf(actorId),
                "sigilo=" + (effectiveSigilo == null ? "PUBLICO" : effectiveSigilo.name()),
                "risk=" + (riskLevel == null ? PjbAuthorizationRiskLevel.BAIXO.name() : riskLevel.name()) + ':' + riskScore,
                "stepUp=" + renderStepUp(),
                "governance=" + renderGovernance(),
                "requestId=" + normalize(requestId)
        );
    }

    private String renderStepUp() {
        if (stepUp == null) {
            return "none";
        }
        return normalize(stepUp.channel()) + ':' + normalize(stepUp.code()) + ':' + stepUp.required() + ':' + stepUp.satisfied();
    }

    private String renderGovernance() {
        if (governance == null) {
            return "none";
        }
        return normalize(governance.channel()) + ':' + normalize(governance.code()) + ':' + normalize(governance.scope()) + ':' + governance.required() + ':' + governance.satisfied();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        return value.trim().replace(';', '_').replace('|', '_').replace("\n", " ").replace("\r", " ").toUpperCase(Locale.ROOT);
    }
}
