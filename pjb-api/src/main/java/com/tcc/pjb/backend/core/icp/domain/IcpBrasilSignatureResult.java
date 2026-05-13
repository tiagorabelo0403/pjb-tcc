package com.tcc.pjb.backend.core.icp.domain;

import java.security.cert.X509Certificate;

public record IcpBrasilSignatureResult(
        byte[] signature,
        X509Certificate signerCertificate,
        String profileCandidate,
        String profileAchieved,
        boolean tsRfc3161Embedded,
        boolean dssDictPresent,
        boolean vriPresent,
        String algorithm
) {
    public IcpBrasilSignatureResult {
        signature = signature == null ? new byte[0] : signature.clone();
    }

    public byte[] signatureCopy() {
        return signature.clone();
    }
}
