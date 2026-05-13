package com.tcc.pjb.backend.integration.judicial.security;

import java.time.Duration;

public record JudicialCertificateValidationPolicy(
        String environmentName,
        JudicialCertificateRevocationMode revocationMode,
        boolean ocspEnabled,
        boolean crlEnabled,
        boolean preferCrl,
        Duration minimumRemainingValidity,
        Duration allowedClockSkew,
        boolean requireDigitalSignatureKeyUsage,
        boolean requireClientAuthExtendedKeyUsage,
        boolean requireTrustStoreForPathValidation
) {
    public boolean revocationEnabled() {
        return revocationMode != null && revocationMode != JudicialCertificateRevocationMode.DISABLED && (ocspEnabled || crlEnabled);
    }
}
