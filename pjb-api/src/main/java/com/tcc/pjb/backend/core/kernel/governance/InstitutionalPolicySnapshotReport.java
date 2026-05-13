package com.tcc.pjb.backend.core.kernel.governance;

import java.util.List;
import java.util.Map;

public record InstitutionalPolicySnapshotReport(
        String scope,
        String status,
        double confidence,
        String policyKey,
        String policyTier,
        String policyVersion,
        boolean approvalRequired,
        boolean strictRelease,
        List<String> mandatoryDirectives,
        List<String> blockingDirectives,
        List<String> releaseGuardrails,
        List<String> escalationTriggers,
        Map<String, Object> diagnostics,
        InstitutionalPolicyAxisReport policyAxes
) {

    public InstitutionalPolicySnapshotReport(
            String scope,
            String status,
            double confidence,
            String policyKey,
            String policyTier,
            String policyVersion,
            boolean approvalRequired,
            boolean strictRelease,
            List<String> mandatoryDirectives,
            List<String> blockingDirectives,
            List<String> releaseGuardrails,
            List<String> escalationTriggers,
            Map<String, Object> diagnostics
    ) {
        this(
                scope,
                status,
                confidence,
                policyKey,
                policyTier,
                policyVersion,
                approvalRequired,
                strictRelease,
                mandatoryDirectives,
                blockingDirectives,
                releaseGuardrails,
                escalationTriggers,
                diagnostics,
                InstitutionalPolicyAxisReport.empty()
        );
    }
}
