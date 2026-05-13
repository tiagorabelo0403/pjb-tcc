package com.tcc.pjb.backend.core.processual.ato;

public record AtoProcessualSecurityProfile(
        boolean requiresStepUp,
        boolean requiresBindingCheck,
        boolean requiresSemanticHash,
        boolean requiresQuantumSignature,
        boolean requiresImmutableAudit,
        boolean requiresHumanReason,
        boolean requiresCrossCheck,
        boolean requiresFreshFocus,
        String securityAction
) {

    public static AtoProcessualSecurityProfile standard() {
        return new AtoProcessualSecurityProfile(false, false, false, false, true, false, false, false, "WRITE_CASE");
    }

    public static AtoProcessualSecurityProfile reinforced() {
        return new AtoProcessualSecurityProfile(true, true, true, false, true, false, true, true, "WRITE_JUDICIAL_ACT");
    }

    public static AtoProcessualSecurityProfile sovereignDecision() {
        return new AtoProcessualSecurityProfile(true, true, true, true, true, true, true, true, "WRITE_JUDICIAL_ACT");
    }

    public static AtoProcessualSecurityProfile publication() {
        return new AtoProcessualSecurityProfile(true, true, true, false, true, true, true, false, "PUBLISH_JUDICIAL_ACT");
    }

    public static AtoProcessualSecurityProfile terminalTransition() {
        return new AtoProcessualSecurityProfile(true, true, true, false, true, true, true, true, "ARCHIVE_CASE");
    }

    public static AtoProcessualSecurityProfile executionTransition() {
        return new AtoProcessualSecurityProfile(true, true, true, false, true, true, false, true, "EXECUTE_JUDICIAL_ACT");
    }

    public static AtoProcessualSecurityProfile sensitiveMandate() {
        return new AtoProcessualSecurityProfile(true, true, true, false, true, true, true, true, "ISSUE_MANDATE");
    }

    public static AtoProcessualSecurityProfile releaseOrder() {
        return new AtoProcessualSecurityProfile(true, true, true, true, true, true, true, true, "ISSUE_RELEASE_ORDER");
    }

    public boolean requiresElevatedSecurity() {
        return requiresStepUp || requiresBindingCheck || requiresSemanticHash || requiresQuantumSignature;
    }
}
