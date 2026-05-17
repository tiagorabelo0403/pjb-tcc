package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorCertificateValidationService {

    private static final String CLIENT_AUTH_OID = "1.3.6.1.5.5.7.3.2";

    private final JudicialConnectorSecurityProperties properties;
    private final JudicialConnectorSecurityPackService securityPackService;
    private final JudicialConnectorCryptographicContextService cryptographicContextService;
    private final JudicialKeyStoreLoader keyStoreLoader;
    private final JudicialConnectorLowLevelSecurityAuditService auditService;
    private final JudicialConnectorSecurityTelemetryService telemetryService;
    private final Clock clock;

    @Inject
    public JudicialConnectorCertificateValidationService(JudicialConnectorSecurityProperties properties,
                                                         JudicialConnectorSecurityPackService securityPackService,
                                                         JudicialConnectorCryptographicContextService cryptographicContextService,
                                                         JudicialKeyStoreLoader keyStoreLoader,
                                                         JudicialConnectorLowLevelSecurityAuditService auditService,
                                                         JudicialConnectorSecurityTelemetryService telemetryService) {
        this(properties, securityPackService, cryptographicContextService, keyStoreLoader, auditService, telemetryService, Clock.systemUTC());
    }

    JudicialConnectorCertificateValidationService(JudicialConnectorSecurityProperties properties,
                                                  JudicialConnectorSecurityPackService securityPackService,
                                                  JudicialConnectorCryptographicContextService cryptographicContextService,
                                                  JudicialKeyStoreLoader keyStoreLoader,
                                                  JudicialConnectorLowLevelSecurityAuditService auditService,
                                                  JudicialConnectorSecurityTelemetryService telemetryService,
                                                  Clock clock) {
        this.properties = Objects.requireNonNull(properties);
        this.securityPackService = Objects.requireNonNull(securityPackService);
        this.cryptographicContextService = Objects.requireNonNull(cryptographicContextService);
        this.keyStoreLoader = Objects.requireNonNull(keyStoreLoader);
        this.auditService = Objects.requireNonNull(auditService);
        this.telemetryService = Objects.requireNonNull(telemetryService);
        this.clock = Objects.requireNonNull(clock);
    }

    public JudicialCertificateValidationPolicy validationPolicy() {
        return validationPolicy(null, null, null, null, Map.of());
    }

    public JudicialCertificateValidationPolicy validationPolicy(JudicialSystem system,
                                                                String tribunalCodigo,
                                                                URI targetUri,
                                                                JudicialIntegrationProperties.Connector connectorConfig,
                                                                Map<String, Object> metadata) {
        return securityPackService.resolve(system, tribunalCodigo, targetUri, connectorConfig, metadata).validationPolicy();
    }

    public JudicialCertificateValidationReport validate(JudicialSystem system,
                                                        String tribunalCodigo,
                                                        URI targetUri,
                                                        JudicialIntegrationProperties.Connector connectorConfig,
                                                        Map<String, Object> metadata) {
        JudicialResolvedSecurityBinding binding = cryptographicContextService.resolveBinding(system, tribunalCodigo, targetUri, connectorConfig, metadata);
        return validate(system, tribunalCodigo, targetUri, connectorConfig, binding, metadata);
    }

    public JudicialCertificateValidationReport validate(JudicialSystem system,
                                                        String tribunalCodigo,
                                                        JudicialResolvedSecurityBinding binding,
                                                        Map<String, Object> metadata) {
        return validate(system, tribunalCodigo, null, null, binding, metadata);
    }

    public JudicialCertificateValidationReport validate(JudicialSystem system,
                                                        String tribunalCodigo,
                                                        URI targetUri,
                                                        JudicialIntegrationProperties.Connector connectorConfig,
                                                        JudicialResolvedSecurityBinding binding,
                                                        Map<String, Object> metadata) {
        JudicialCertificateValidationPolicy policy = validationPolicy(system, tribunalCodigo, targetUri, connectorConfig, metadata);
        Instant now = clock.instant();
        JudicialKeyStoreMaterial keyStoreMaterial = binding.keyStoreRef() != null ? keyStoreLoader.loadKeyStore(binding.keyStoreRef()) : null;
        JudicialKeyStoreMaterial trustStoreMaterial = binding.trustStoreRef() != null ? keyStoreLoader.loadTrustStore(binding.trustStoreRef()) : null;
        String alias = firstNonBlank(binding.keyAlias(), keyStoreMaterial != null ? keyStoreMaterial.preferredAlias() : null);
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashMap<String, Object> reportMetadata = new LinkedHashMap<>();
        reportMetadata.put("tlsMode", binding.tlsMode() != null ? binding.tlsMode().name() : null);
        reportMetadata.put("bindingId", binding.bindingId());
        reportMetadata.put("environmentName", binding.environmentName());
        reportMetadata.putAll(binding.metadata());
        reportMetadata.put("targetHost", targetUri != null ? targetUri.getHost() : null);
        reportMetadata.put("trustStorePresent", trustStoreMaterial != null);
        if (keyStoreMaterial == null) {
            blockers.add("CERTIFICATE_KEYSTORE_UNAVAILABLE");
            JudicialCertificateValidationReport report = new JudicialCertificateValidationReport(
                    now,
                    system,
                    tribunalCodigo,
                    binding.bindingId(),
                    binding.environmentName(),
                    binding.keyStoreRef(),
                    binding.trustStoreRef(),
                    alias,
                    false,
                    false,
                    false,
                    false,
                    false,
                    trustStoreMaterial != null,
                    false,
                    false,
                    false,
                    false,
                    false,
                    "BLOCKED",
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    List.copyOf(blockers),
                    List.copyOf(warnings),
                    Map.copyOf(cleanMap(reportMetadata))
            );
            telemetryService.recordCertificateValidation(system, tribunalCodigo, report.status(), false, false);
            return report;
        }
        X509Certificate leaf = resolveLeafCertificate(keyStoreMaterial.keyStore(), alias);
        List<X509Certificate> chain = resolveChain(keyStoreMaterial.keyStore(), alias, leaf);
        boolean certificatePresent = leaf != null;
        if (!certificatePresent) {
            blockers.add("CERTIFICATE_ALIAS_UNRESOLVED");
        }
        Instant notBefore = leaf != null ? leaf.getNotBefore().toInstant() : null;
        Instant notAfter = leaf != null ? leaf.getNotAfter().toInstant() : null;
        Duration remainingValidity = notAfter != null ? Duration.between(now, notAfter) : null;
        boolean expired = notAfter != null && notAfter.isBefore(now.minus(policy.allowedClockSkew()));
        boolean validNow = leaf != null && !expired && notBefore != null && !notBefore.isAfter(now.plus(policy.allowedClockSkew()));
        boolean expiresSoon = validNow && remainingValidity != null && remainingValidity.compareTo(policy.minimumRemainingValidity()) <= 0;
        if (leaf != null) {
            if (!validNow) {
                blockers.add(expired ? "CERTIFICATE_EXPIRED" : "CERTIFICATE_NOT_YET_VALID");
            }
            if (expiresSoon) {
                warnings.add("CERTIFICATE_EXPIRING_SOON");
            }
            if (policy.requireDigitalSignatureKeyUsage() && !supportsDigitalSignature(leaf)) {
                blockers.add("CERTIFICATE_KEY_USAGE_DIGITAL_SIGNATURE_MISSING");
            }
            if (policy.requireClientAuthExtendedKeyUsage() && !supportsClientAuthentication(leaf)) {
                blockers.add("CERTIFICATE_EXTENDED_KEY_USAGE_CLIENT_AUTH_MISSING");
            }
        }
        boolean pathValidationAttempted = false;
        boolean pathValidationSucceeded = false;
        boolean revocationAttempted = false;
        boolean revocationSoftFailed = false;
        boolean revocationHardFailed = false;
        if (leaf != null) {
            try {
                Set<TrustAnchor> trustAnchors = trustStoreMaterial != null ? trustAnchors(trustStoreMaterial.keyStore()) : Set.of();
                if (!trustAnchors.isEmpty()) {
                    if (containsTrustAnchorCertificate(leaf, trustAnchors)) {
                        pathValidationSucceeded = true;
                        pathValidationAttempted = true;
                    } else {
                        pathValidationAttempted = true;
                        PKIXParameters parameters = new PKIXParameters(trustAnchors);
                        parameters.setRevocationEnabled(policy.revocationEnabled());
                        parameters.setDate(Date.from(now));
                        if (policy.revocationEnabled()) {
                            PKIXRevocationChecker checker = (PKIXRevocationChecker) CertPathValidator.getInstance("PKIX").getRevocationChecker();
                            LinkedHashSet<PKIXRevocationChecker.Option> options = new LinkedHashSet<>();
                            if (policy.preferCrl()) {
                                options.add(PKIXRevocationChecker.Option.PREFER_CRLS);
                            }
                            if (policy.revocationMode() == JudicialCertificateRevocationMode.SOFT_FAIL) {
                                options.add(PKIXRevocationChecker.Option.SOFT_FAIL);
                            }
                            if (policy.crlEnabled() && !policy.ocspEnabled()) {
                                options.add(PKIXRevocationChecker.Option.NO_FALLBACK);
                            }
                            checker.setOptions(Set.copyOf(options));
                            parameters.addCertPathChecker(checker);
                            revocationAttempted = true;
                        }
                        CertPath certPath = java.security.cert.CertificateFactory.getInstance("X.509").generateCertPath(chainWithoutTrustAnchor(chain, trustAnchors));
                        PKIXCertPathValidatorResult ignored = (PKIXCertPathValidatorResult) CertPathValidator.getInstance("PKIX").validate(certPath, parameters);
                        pathValidationSucceeded = true;
                    }
                } else if (policy.requireTrustStoreForPathValidation()) {
                    blockers.add("CERTIFICATE_TRUSTSTORE_UNAVAILABLE");
                } else {
                    warnings.add("CERTIFICATE_TRUSTSTORE_UNAVAILABLE");
                }
            } catch (CertPathValidatorException ex) {
                if (policy.revocationMode() == JudicialCertificateRevocationMode.SOFT_FAIL && policy.revocationEnabled()) {
                    revocationSoftFailed = true;
                    warnings.add("CERTIFICATE_REVOCATION_SOFT_FAILED");
                } else {
                    revocationHardFailed = policy.revocationEnabled();
                    blockers.add("CERTIFICATE_PATH_VALIDATION_FAILED");
                }
                auditService.recordContextFailure(system, tribunalCodigo, null, "CERTIFICATE_VALIDATION", binding, ex, correlationId(metadata), Map.of("bindingId", binding.bindingId()));
            } catch (Exception ex) {
                blockers.add(policy.revocationEnabled() ? "CERTIFICATE_VALIDATION_EXECUTION_FAILED" : "CERTIFICATE_PATH_VALIDATION_FAILED");
                revocationHardFailed = policy.revocationEnabled();
                auditService.recordContextFailure(system, tribunalCodigo, null, "CERTIFICATE_VALIDATION", binding, ex, correlationId(metadata), Map.of("bindingId", binding.bindingId()));
            }
        }
        String fingerprint = leaf != null ? sha256(leaf) : null;
        reportMetadata.put("subjectAlternativeNamesPresent", leaf != null && hasSubjectAlternativeNames(leaf));
        reportMetadata.put("issuerAlternativeNamesPresent", leaf != null && hasIssuerAlternativeNames(leaf));
        reportMetadata.put("certificateType", leaf != null ? leaf.getType() : null);
        reportMetadata.put("revocationMode", policy.revocationMode() != null ? policy.revocationMode().name() : null);
        reportMetadata.put("ocspEnabled", policy.ocspEnabled());
        reportMetadata.put("crlEnabled", policy.crlEnabled());
        reportMetadata.put("metadataSize", metadata == null ? 0 : metadata.size());
        String status = blockers.isEmpty()
                ? warnings.isEmpty() ? "VALID" : "WARNINGS"
                : "BLOCKED";
        JudicialCertificateValidationReport report = new JudicialCertificateValidationReport(
                now,
                system,
                tribunalCodigo,
                binding.bindingId(),
                binding.environmentName(),
                binding.keyStoreRef(),
                binding.trustStoreRef(),
                alias,
                certificatePresent,
                keyStoreMaterial.hardwareBacked(),
                validNow,
                expiresSoon,
                expired,
                trustStoreMaterial != null,
                pathValidationAttempted,
                pathValidationSucceeded,
                revocationAttempted,
                revocationSoftFailed,
                revocationHardFailed,
                status,
                notBefore,
                notAfter,
                remainingValidity,
                chain.size(),
                leaf != null ? leaf.getSubjectX500Principal().getName() : null,
                leaf != null ? leaf.getIssuerX500Principal().getName() : null,
                leaf != null ? toHex(leaf.getSerialNumber()) : null,
                fingerprint,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(cleanMap(reportMetadata))
        );
        telemetryService.recordCertificateValidation(system, tribunalCodigo, report.status(), report.hardwareBacked(), report.revocationAttempted());
        return report;
    }

    private boolean supportsDigitalSignature(X509Certificate certificate) {
        boolean[] usage = certificate.getKeyUsage();
        if (usage == null || usage.length == 0) {
            return true;
        }
        return usage[0] || usage.length > 1 && usage[1];
    }

    private boolean supportsClientAuthentication(X509Certificate certificate) {
        try {
            List<String> extendedKeyUsage = certificate.getExtendedKeyUsage();
            return extendedKeyUsage == null || extendedKeyUsage.contains(CLIENT_AUTH_OID);
        } catch (CertificateException ex) {
            return false;
        }
    }

    private Set<TrustAnchor> trustAnchors(KeyStore trustStore) {
        try {
            LinkedHashSet<TrustAnchor> anchors = new LinkedHashSet<>();
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate certificate = trustStore.getCertificate(alias);
                if (certificate instanceof X509Certificate x509Certificate) {
                    anchors.add(new TrustAnchor(x509Certificate, null));
                }
            }
            return Set.copyOf(anchors);
        } catch (Exception ex) {
            throw new JudicialConnectorCryptographicException("Unable to enumerate trust anchors for judicial connector validation.", ex);
        }
    }

    private boolean containsTrustAnchorCertificate(X509Certificate certificate, Set<TrustAnchor> anchors) {
        return anchors.stream().map(TrustAnchor::getTrustedCert).filter(Objects::nonNull).anyMatch(certificate::equals);
    }

    private List<X509Certificate> chainWithoutTrustAnchor(List<X509Certificate> chain, Set<TrustAnchor> anchors) {
        if (chain.isEmpty()) {
            return List.of();
        }
        ArrayList<X509Certificate> out = new ArrayList<>(chain);
        if (anchors.stream().map(TrustAnchor::getTrustedCert).filter(Objects::nonNull).anyMatch(out.getLast()::equals)) {
            out.removeLast();
        }
        return List.copyOf(out);
    }

    private X509Certificate resolveLeafCertificate(KeyStore keyStore, String alias) {
        try {
            if (alias != null) {
                Certificate direct = keyStore.getCertificate(alias);
                if (direct instanceof X509Certificate x509Certificate) {
                    return x509Certificate;
                }
            }
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String currentAlias = aliases.nextElement();
                Certificate certificate = keyStore.getCertificate(currentAlias);
                if (certificate instanceof X509Certificate x509Certificate) {
                    return x509Certificate;
                }
            }
            return null;
        } catch (Exception ex) {
            throw new JudicialConnectorCryptographicException("Unable to resolve certificate alias for judicial connector validation.", ex);
        }
    }

    private List<X509Certificate> resolveChain(KeyStore keyStore, String alias, X509Certificate leaf) {
        try {
            if (alias != null) {
                Certificate[] chain = keyStore.getCertificateChain(alias);
                if (chain != null && chain.length > 0) {
                    ArrayList<X509Certificate> out = new ArrayList<>();
                    for (Certificate certificate : chain) {
                        if (certificate instanceof X509Certificate x509Certificate) {
                            out.add(x509Certificate);
                        }
                    }
                    if (!out.isEmpty()) {
                        return List.copyOf(out);
                    }
                }
            }
            return leaf == null ? List.of() : List.of(leaf);
        } catch (Exception ex) {
            throw new JudicialConnectorCryptographicException("Unable to resolve certificate chain for judicial connector validation.", ex);
        }
    }

    private String sha256(X509Certificate certificate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(certificate.getEncoded()));
        } catch (CertificateEncodingException | java.security.NoSuchAlgorithmException ex) {
            throw new JudicialConnectorCryptographicException("Unable to compute certificate fingerprint.", ex);
        }
    }

    private String toHex(BigInteger serial) {
        return serial == null ? null : serial.toString(16).toUpperCase(Locale.ROOT);
    }

    private boolean hasSubjectAlternativeNames(X509Certificate certificate) {
        try {
            Collection<List<?>> names = certificate.getSubjectAlternativeNames();
            return names != null && !names.isEmpty();
        } catch (CertificateException ex) {
            return false;
        }
    }

    private boolean hasIssuerAlternativeNames(X509Certificate certificate) {
        try {
            Collection<List<?>> names = certificate.getIssuerAlternativeNames();
            return names != null && !names.isEmpty();
        } catch (CertificateException ex) {
            return false;
        }
    }

    private String correlationId(Map<String, Object> metadata) {
        return firstNonBlank(stringValue(metadata != null ? metadata.get("requestId") : null), stringValue(metadata != null ? metadata.get("idempotencyKey") : null));
    }

    private Map<String, Object> cleanMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? null : normalized;
    }
}
