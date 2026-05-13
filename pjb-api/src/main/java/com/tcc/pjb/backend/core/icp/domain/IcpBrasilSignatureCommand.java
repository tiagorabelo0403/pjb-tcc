package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilSignatureCommand(
        String keyStoreReference,
        String keyAlias,
        char[] keyPassword,
        String profileCandidate
) {
    public IcpBrasilSignatureCommand {
        keyPassword = keyPassword == null ? null : keyPassword.clone();
    }

    public char[] keyPasswordCopy() {
        return keyPassword == null ? null : keyPassword.clone();
    }
}
