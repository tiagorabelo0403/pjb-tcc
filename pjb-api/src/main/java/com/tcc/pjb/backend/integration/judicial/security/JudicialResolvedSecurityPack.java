package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record JudicialResolvedSecurityPack(
        String packId,
        JudicialSystem system,
        String tribunalCodigo,
        String environmentName,
        boolean enabled,
        JudicialConnectorTlsMode tlsMode,
        String keyStoreRef,
        String trustStoreRef,
        String keyAlias,
        boolean requireClientCertificate,
        boolean hostnameVerification,
        Duration connectTimeout,
        Duration readTimeout,
        List<String> protocols,
        List<String> cipherSuites,
        List<String> allowedHosts,
        JudicialCertificateRevocationMode revocationMode,
        boolean ocspEnabled,
        boolean crlEnabled,
        boolean preferCrl,
        Duration minimumRemainingValidity,
        Duration allowedClockSkew,
        boolean requireDigitalSignatureKeyUsage,
        boolean requireClientAuthExtendedKeyUsage,
        boolean requireTrustStoreForPathValidation,
        Map<String, Object> metadata
) {
    public JudicialResolvedSecurityPack {
        protocols = protocols == null ? List.of() : List.copyOf(protocols);
        cipherSuites = cipherSuites == null ? List.of() : List.copyOf(cipherSuites);
        allowedHosts = allowedHosts == null ? List.of() : List.copyOf(allowedHosts);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public JudicialCertificateValidationPolicy validationPolicy() {
        return new JudicialCertificateValidationPolicy(
                environmentName,
                revocationMode,
                ocspEnabled,
                crlEnabled,
                preferCrl,
                minimumRemainingValidity,
                allowedClockSkew,
                requireDigitalSignatureKeyUsage,
                requireClientAuthExtendedKeyUsage,
                requireTrustStoreForPathValidation
        );
    }
}
