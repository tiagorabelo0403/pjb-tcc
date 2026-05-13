package com.tcc.pjb.backend.core.security.device.policy;

public record SecurityActionPolicy(
        boolean deviceRequired,
        boolean verifiedDeviceRequired,
        boolean attestationTrustedRequired,
        boolean advogadoBaptismRequired,
        boolean passkeyRequiredForAdvogado,
        boolean passkeyRequiredForAdmin,
        boolean allowReadDuringQuarantine,
        boolean allowWriteDuringQuarantine,
        boolean justificativaRequired,
        boolean stepUpRequired,
        int strongAuthMaxAgeSeconds,
        boolean bindStrongAuthToDevice,
        boolean oneTimeStepUp,
        boolean dualApprovalRequired,
        int dualApprovalTtlSeconds,
        boolean auditRequired,
        boolean govBrRequired,
        int govBrMaxAgeSeconds
) {
}
