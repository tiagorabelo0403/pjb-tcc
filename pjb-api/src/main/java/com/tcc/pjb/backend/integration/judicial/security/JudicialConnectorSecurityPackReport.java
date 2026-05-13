package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorSecurityPackReport(
        Instant generatedAt,
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
    public JudicialConnectorSecurityPackReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        protocols = protocols == null ? List.of() : List.copyOf(protocols);
        cipherSuites = cipherSuites == null ? List.of() : List.copyOf(cipherSuites);
        allowedHosts = allowedHosts == null ? List.of() : List.copyOf(allowedHosts);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("packId", packId);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("environmentName", environmentName);
        out.put("enabled", enabled);
        out.put("tlsMode", tlsMode != null ? tlsMode.name() : null);
        out.put("keyStoreRef", keyStoreRef);
        out.put("trustStoreRef", trustStoreRef);
        out.put("keyAlias", keyAlias);
        out.put("requireClientCertificate", requireClientCertificate);
        out.put("hostnameVerification", hostnameVerification);
        out.put("connectTimeoutMs", connectTimeout != null ? connectTimeout.toMillis() : null);
        out.put("readTimeoutMs", readTimeout != null ? readTimeout.toMillis() : null);
        out.put("protocols", protocols);
        out.put("cipherSuites", cipherSuites);
        out.put("allowedHosts", allowedHosts);
        out.put("revocationMode", revocationMode != null ? revocationMode.name() : null);
        out.put("ocspEnabled", ocspEnabled);
        out.put("crlEnabled", crlEnabled);
        out.put("preferCrl", preferCrl);
        out.put("minimumRemainingValiditySeconds", minimumRemainingValidity != null ? minimumRemainingValidity.toSeconds() : null);
        out.put("allowedClockSkewSeconds", allowedClockSkew != null ? allowedClockSkew.toSeconds() : null);
        out.put("requireDigitalSignatureKeyUsage", requireDigitalSignatureKeyUsage);
        out.put("requireClientAuthExtendedKeyUsage", requireClientAuthExtendedKeyUsage);
        out.put("requireTrustStoreForPathValidation", requireTrustStoreForPathValidation);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
