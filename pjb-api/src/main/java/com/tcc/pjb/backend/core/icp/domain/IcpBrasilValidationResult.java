package com.tcc.pjb.backend.core.icp.domain;

import com.tcc.pjb.backend.core.icp.IcpBrasilCertProfile;

public record IcpBrasilValidationResult(
        boolean valid,
        IcpBrasilCertProfile profile,
        String failureReason
) {
    public static IcpBrasilValidationResult ok(IcpBrasilCertProfile profile) {
        return new IcpBrasilValidationResult(true, profile, null);
    }

    public static IcpBrasilValidationResult fail(String failureReason) {
        return new IcpBrasilValidationResult(false, null, failureReason);
    }
}
